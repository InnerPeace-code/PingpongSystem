package org.example.pingpongsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.pingpongsystem.entity.*;
import org.example.pingpongsystem.repository.*;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseAppointmentService {

    private final CourseAppointmentRepository appointmentRepository;
    private final CoachTeachStudentRepository relationRepository;
    private final TableRepository tableRepository;
    private final CancelRecordRepository cancelRecordRepository;
    private final PaymentService paymentService;
    private final CoachRepository coachRepository;
    private final StudentAccountRepository studentAccountRepository;
    private final PaymentRecordRepository paymentRecordRepository;

    public Result<List<CourseAppointmentEntity>> getCoachSchedule(Long coachId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfWeek = now.plusWeeks(1);
        // 定义需要排除的状态列表：已取消和已拒绝
        List<CourseAppointmentEntity.AppointmentStatus> excludedStatuses = List.of(
                CourseAppointmentEntity.AppointmentStatus.CANCELLED,
                CourseAppointmentEntity.AppointmentStatus.REJECTED
        );
        // 调用新增的查询方法，排除指定状态
        List<CourseAppointmentEntity> schedule = appointmentRepository
                .findByCoachIdAndStartTimeBetweenAndStatusNotIn(
                        coachId, now, endOfWeek, excludedStatuses);
        return Result.success(schedule);
    }

    @Transactional
    public Result<CourseAppointmentEntity> bookCourse(
            Long coachId, Long studentId, LocalDateTime startTime,
            LocalDateTime endTime, Long tableId, boolean autoAssign) {

        // 1. 验证双选关系
        if (!relationRepository.existsByCoachIdAndStudentIdAndIsConfirmed(coachId, studentId, true)) {
            return Result.error(StatusCode.FAIL, "未与该教练建立双选关系");
        }

        // 2. 调整时间有效性验证：仅要求结束时间晚于开始时间，且时长不超过合理上限（如8小时，可按需调整）
        // 去掉1-2小时的严格限制，改为基础有效性+合理上限
        if (endTime.isBefore(startTime)) {
            return Result.error(StatusCode.FAIL, "结束时间不能早于开始时间");
        }

        // 3. 获取教练所在校区，用于球台筛选
        Optional<CoachEntity> coachOpt = coachRepository.findById(coachId);
        if (coachOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "教练不存在");
        }
        CoachEntity coach = coachOpt.get();
        Long schoolId = coach.getSchoolId();
        if (schoolId == null) {
            return Result.error(StatusCode.FAIL, "教练未分配校区");
        }

        // 4. 分配球台（根据校区筛选）
        Long finalTableId;
        if (autoAssign) {
            finalTableId = autoAssignTable(schoolId, startTime, endTime);
        } else {
            // 验证手动选择的球台是否属于该校区且可用
            if (!isTableAvailableInSchool(tableId, schoolId, startTime, endTime)) {
                return Result.error(StatusCode.FAIL, "所选球台不可用或不属于该教练所在校区");
            }
            finalTableId = tableId;
        }

        if (finalTableId == null) {
            return Result.error(StatusCode.FAIL, "无可用球台");
        }

        // 5. 根据教练等级计算费用
        int level = coach.getLevel();
        double hourlyRate;

        switch (level) {
            case 10:  // 初级教练
                hourlyRate = 80.0;
                break;
            case 100:  // 中级教练
                hourlyRate = 150.0;
                break;
            case 1000:  // 高级教练
                hourlyRate = 200.0;
                break;
            default:
                return Result.error(StatusCode.FAIL, "教练等级设置异常");
        }

        // 计算总费用（时长小时数 × 时薪）
        long hours = java.time.Duration.between(startTime, endTime).toHours();
        double totalAmount = hourlyRate * hours;

        // ---------------------- 新增：余额检查与扣款逻辑 ----------------------
        // 6. 查询学生账户
        Optional<StudentAccountEntity> accountOpt = studentAccountRepository.findByStudentId(studentId);
        if (accountOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "学生账户不存在，请先开通账户");
        }
        StudentAccountEntity account = accountOpt.get();
        // 7. 检查余额是否充足
        if (account.getBalance() < totalAmount) {
            return Result.error(StatusCode.FAIL,
                    String.format("余额不足，当前余额: %.2f元，所需金额: %.2f元",
                            account.getBalance(), totalAmount));
        }

        // 8. 创建支付记录（账户余额支付）- 金额为负数表示消费
        PaymentRecordEntity savedRecord = paymentService.createCoursePaymentRecord(studentId, totalAmount);

// 9. 扣减余额（使用乐观锁防止并发问题）
        try {
            account.setBalance(account.getBalance() - totalAmount);
            studentAccountRepository.save(account); // 乐观锁会自动检查version
        } catch (OptimisticLockingFailureException e) {
            // 并发更新冲突时重试（可根据实际需求调整重试逻辑）
            return Result.error(StatusCode.FAIL, "操作过于频繁，请稍后重试");
        }


        // ---------------------- 原有预约创建逻辑调整 ----------------------
        // 10. 创建预约记录（状态改为等待确认）
        CourseAppointmentEntity appointment = new CourseAppointmentEntity();
        appointment.setCoachId(coachId);
        appointment.setStudentId(studentId);
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setTableId(finalTableId);
        appointment.setStatus(CourseAppointmentEntity.AppointmentStatus.PENDING_CONFIRM); // 等待教练确认
        appointment.setAmount(totalAmount);
        appointment.setSchoolId(schoolId);
        appointment.setPaymentRecordId(savedRecord.getId()); // 关联支付记录ID

        appointmentRepository.save(appointment);
        return Result.success(appointment);
    }

    @Transactional
    public Result<String> handleCoachConfirmation(Long appointmentId, boolean accept) {
        Optional<CourseAppointmentEntity> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "预约记录不存在");
        }

        CourseAppointmentEntity appointment = appointmentOpt.get();
        if (accept) {
            appointment.setStatus(CourseAppointmentEntity.AppointmentStatus.CONFIRMED);
            appointmentRepository.save(appointment);
            return Result.success("已确认预约");
        } else {
            // 拒绝预约时，触发退款
            if (appointment.getPaymentRecordId() != null) {
                // 调用PaymentService的退款方法
                Result<String> refundResult = paymentService.refund(appointment.getPaymentRecordId());
                if (!refundResult.isSuccess()) {
                    return refundResult; // 退款失败时返回错误
                }
            }
            appointment.setStatus(CourseAppointmentEntity.AppointmentStatus.REJECTED);
            appointmentRepository.save(appointment);
            return Result.success("已拒绝预约，已自动退款");
        }
    }

    @Transactional
    public Result<String> requestCancel(Long appointmentId, Long userId, String userType) {
        // 1. 验证预约存在
        Optional<CourseAppointmentEntity> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "预约记录不存在");
        }

        // 2. 验证取消时间（需提前24小时）
        CourseAppointmentEntity appointment = appointmentOpt.get();
        if (LocalDateTime.now().plusHours(24).isAfter(appointment.getStartTime())) {
            return Result.error(StatusCode.FAIL, "需提前24小时取消预约");
        }

        // 3. 验证本月取消次数（最多3次，只统计PENDING和APPROVED状态）
        LocalDateTime monthStart = LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth());
        LocalDateTime monthEnd = LocalDateTime.now().with(TemporalAdjusters.lastDayOfMonth());
        List<CancelRecordEntity.CancelStatus> validStatuses = List.of(
                CancelRecordEntity.CancelStatus.PENDING,
                CancelRecordEntity.CancelStatus.APPROVED
        );

        long cancelCount = 0;
        // 根据发起人类型统计取消次数
        if ("STUDENT".equals(userType)) {
            cancelCount = cancelRecordRepository.countByStudentIdAndUserTypeAndStatusInAndCreateTimeBetween(
                    userId, userType, validStatuses, monthStart, monthEnd);
        } else if ("COACH".equals(userType)) {
            cancelCount = cancelRecordRepository.countByCoachIdAndUserTypeAndStatusInAndCreateTimeBetween(
                    userId, userType, validStatuses, monthStart, monthEnd);
        }

        if (cancelCount >= 3) {
            return Result.error(StatusCode.FAIL, "本月取消次数已达上限（3次）");
        }

        // 4. 创建取消申请（设置双方ID）
        CancelRecordEntity cancelRecord = new CancelRecordEntity();
        cancelRecord.setStudentId(appointment.getStudentId());
        cancelRecord.setCoachId(appointment.getCoachId());
        cancelRecord.setUserType(userType);  // 记录发起人类型
        cancelRecord.setAppointmentId(appointmentId);
        cancelRecord.setCreateTime(LocalDateTime.now());
        cancelRecord.setStatus(CancelRecordEntity.CancelStatus.PENDING);
        cancelRecordRepository.save(cancelRecord);

        appointment.setStatus(CourseAppointmentEntity.AppointmentStatus.CANCEL_REQUESTED);
        appointmentRepository.save(appointment);

        return Result.success("取消申请已提交，剩余取消次数：" + (3 - cancelCount - 1));
    }

    @Transactional
    public Result<String> handleCancelRequest(Long cancelRecordId, boolean approve) {
        Optional<CancelRecordEntity> recordOpt = cancelRecordRepository.findById(cancelRecordId);
        if (recordOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "取消记录不存在");
        }

        CancelRecordEntity record = recordOpt.get();
        Optional<CourseAppointmentEntity> appointmentOpt = appointmentRepository.findById(record.getAppointmentId());
        if (appointmentOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "关联的预约记录不存在");
        }
        CourseAppointmentEntity appointment = appointmentOpt.get();

        if (approve) {
            // 1. 批准取消：更新取消记录状态为已批准
            record.setStatus(CancelRecordEntity.CancelStatus.APPROVED);
            cancelRecordRepository.save(record);

            // 2. 更新预约状态为“已取消”
            appointment.setStatus(CourseAppointmentEntity.AppointmentStatus.CANCELLED);
            appointmentRepository.save(appointment);

            // 3. 核心：触发退款（若存在支付记录）
            Long paymentRecordId = appointment.getPaymentRecordId();
            if (paymentRecordId != null) {
                Result<String> refundResult = paymentService.refund(paymentRecordId);
                if (!refundResult.isSuccess()) {
                    // 退款失败时，事务会自动回滚（取消状态和预约状态不会保存）
                    return Result.error(StatusCode.FAIL, "取消申请批准成功，但退款失败：" + refundResult.getMessage());
                }
            } else {
                // 理论上不会走到这里（预约时已生成支付记录），但做容错处理
                return Result.error(StatusCode.FAIL, "取消成功，但未找到对应的支付记录，无法退款");
            }

            return Result.success("已批准取消申请，款项已退还");
        } else {
            // 拒绝取消：更新取消记录状态为已拒绝，预约状态恢复为原状态（如CONFIRMED）
            record.setStatus(CancelRecordEntity.CancelStatus.REJECTED);
            cancelRecordRepository.save(record);

            // 恢复预约状态（根据原状态判断，若原状态是PENDING_CONFIRM则保持，若是CONFIRMED则恢复）
            if (appointment.getStatus() == CourseAppointmentEntity.AppointmentStatus.CANCEL_REQUESTED) {
                appointment.setStatus(CourseAppointmentEntity.AppointmentStatus.CONFIRMED);
                appointmentRepository.save(appointment);
            }

            return Result.success("已拒绝取消申请");
        }
    }

    // 自动分配球台（根据校区筛选并检查时间段冲突）
    private Long autoAssignTable(Long schoolId, LocalDateTime startTime, LocalDateTime endTime) {
        // 根据校区查询所有球台
        List<TableEntity> tables = tableRepository.findAllBySchoolId(schoolId);

        // 调试：打印当前校区的所有球台ID
        System.out.println("---------- 调试：当前校区（schoolId=" + schoolId + "）的所有球台ID ----------");
        for (TableEntity table : tables) {
            System.out.println("球台ID：" + table.getId());
        }
        System.out.println("--------------------------------------------------");
        // 查找指定时间段内未被占用的球台
        for (TableEntity table : tables) {
            List<CourseAppointmentEntity> conflicts = appointmentRepository
                    .findByTableIdAndStartTimeLessThanAndEndTimeGreaterThan(
                            table.getId(), endTime, startTime);
            if (conflicts.isEmpty()) {
                return table.getId();
            }
        }
        return null;
    }

    // 验证球台是否属于该校区且在指定时间段可用
    private boolean isTableAvailableInSchool(Long tableId, Long schoolId, LocalDateTime startTime, LocalDateTime endTime) {
        Optional<TableEntity> tableOpt = tableRepository.findById(tableId);
        if (tableOpt.isEmpty()) {
            return false;
        }

        TableEntity table = tableOpt.get();
        // 检查球台是否属于该校区
        if (!table.getSchoolId().equals(schoolId)) {
            return false;
        }

        // 检查球台在指定时间段是否有冲突预约
        List<CourseAppointmentEntity> conflicts = appointmentRepository
                .findByTableIdAndStartTimeLessThanAndEndTimeGreaterThan(
                        tableId, endTime, startTime);
        return conflicts.isEmpty();
    }

    public Result<List<CourseAppointmentEntity>> getStudentAppointments(Long studentId) {
        // 1. 校验参数
        if (studentId == null) {
            return Result.error(StatusCode.FAIL, "学生ID不能为空");
        }

        // 2. 关联查询预约列表（含教练姓名）
        List<CourseAppointmentEntity> appointmentList = appointmentRepository.findByStudentId(studentId);

        // 3. 返回结果
        return Result.success(appointmentList);
    }

    public Result<List<CourseAppointmentEntity>> getCoachAppointments(Long coachId){
        // 1. 校验参数
        if (coachId == null) {
            return Result.error(StatusCode.FAIL, "学生ID不能为空");
        }
        // 2. 关联查询预约列表（含教练姓名）
        List<CourseAppointmentEntity> appointmentList = appointmentRepository.findByCoachId(coachId);
        // 3. 返回结果
        return Result.success(appointmentList);
    }

    public Result<List<CancelRecordEntity>> getPendingCancelRecords(Long userId, String userType) {
        // 获取待处理的取消申请：自己是应答人，对方是发起人，状态为PENDING
        // userType 是发起人, userId 是应答人
        List<CancelRecordEntity> records;
        if ("COACH".equals(userType)) {
            // 学生查询：对方是教练发起的，状态为待处理
            records = cancelRecordRepository.findByStudentIdAndUserTypeAndStatus(
                    userId, "COACH", CancelRecordEntity.CancelStatus.PENDING);
        } else if ("STUDENT".equals(userType)) {
            // 教练查询：对方是学生发起的，状态为待处理
            records = cancelRecordRepository.findByCoachIdAndUserTypeAndStatus(
                    userId, "STUDENT", CancelRecordEntity.CancelStatus.PENDING);
        } else {
            return Result.error(StatusCode.FAIL, "无效的用户类型");
        }
        return Result.success(records);
    }

    // 获取本月剩余取消次数
    public Result<Integer> getRemainingCancelCount(Long userId, String userType) {
        LocalDateTime monthStart = LocalDateTime.now().with(TemporalAdjusters.firstDayOfMonth());
        LocalDateTime monthEnd = LocalDateTime.now().with(TemporalAdjusters.lastDayOfMonth());
        List<CancelRecordEntity.CancelStatus> validStatuses = List.of(
                CancelRecordEntity.CancelStatus.PENDING,
                CancelRecordEntity.CancelStatus.APPROVED
        );

        long cancelCount = 0;
        if ("STUDENT".equals(userType)) {
            cancelCount = cancelRecordRepository.countByStudentIdAndUserTypeAndStatusInAndCreateTimeBetween(
                    userId, userType, validStatuses, monthStart, monthEnd);
        } else if ("COACH".equals(userType)) {
            cancelCount = cancelRecordRepository.countByCoachIdAndUserTypeAndStatusInAndCreateTimeBetween(
                    userId, userType, validStatuses, monthStart, monthEnd);
        } else {
            return Result.error(StatusCode.FAIL, "无效的用户类型");
        }

        int remaining = 3 - (int) cancelCount;
        return Result.success(Math.max(remaining, 0)); // 确保不返回负数
    }
}
