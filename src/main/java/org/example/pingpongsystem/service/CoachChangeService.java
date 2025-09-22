package org.example.pingpongsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.pingpongsystem.entity.*;
import org.example.pingpongsystem.repository.*;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CoachChangeService {

    private final CoachChangeRequestRepository changeRequestRepository;
    private final CoachTeachStudentRepository coachStudentRepository;
    private final StudentRepository studentRepository;
    private final CoachRepository coachRepository;
    private final SchoolRepository schoolRepository;
    private final NotificationService notificationService;

    /**
     * 获取学员当前的教练
     */
    public Result<List<CoachEntity>> getCurrentCoaches(Long studentId) {
        List<CoachTeachStudentEntity> relations = coachStudentRepository
                .findByStudentIdAndIsConfirmed(studentId, true);

        List<CoachEntity> coaches = relations.stream()
                .map(relation -> coachRepository.findById(relation.getCoachId()).orElse(null))
                .filter(coach -> coach != null)
                .toList();

        return Result.success(coaches);
    }

    /**
     * 获取学员所在校区的所有教练
     */
    public Result<List<CoachEntity>> getSchoolCoaches(Long studentId) {
        Optional<StudentEntity> studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "学员不存在");
        }

        Long schoolId = studentOpt.get().getSchoolId();
        List<CoachEntity> coaches = coachRepository.findBySchoolIdAndIsCertifiedTrue(schoolId);
        return Result.success(coaches);
    }

    /**
     * 提交更换教练申请
     */
    @Transactional
    public Result<CoachChangeRequestEntity> submitChangeRequest(
            Long studentId, Long currentCoachId, Long targetCoachId) {

        // 验证学员是否存在
        Optional<StudentEntity> studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "学员不存在");
        }
        StudentEntity student = studentOpt.get();

        // 验证当前教练是否存在且与学员有关联
        Optional<CoachEntity> currentCoachOpt = coachRepository.findById(currentCoachId);
        if (currentCoachOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "当前教练不存在");
        }

        boolean isCurrentCoachValid = coachStudentRepository.existsByCoachIdAndStudentIdAndIsConfirmed(
                currentCoachId, studentId, true);
        if (!isCurrentCoachValid) {
            return Result.error(StatusCode.FAIL, "当前教练与学员无关联");
        }

        // 验证目标教练是否存在
        Optional<CoachEntity> targetCoachOpt = coachRepository.findById(targetCoachId);
        if (targetCoachOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "目标教练不存在");
        }

        // 验证是否为同一校区
        if (!currentCoachOpt.get().getSchoolId().equals(targetCoachOpt.get().getSchoolId())) {
            return Result.error(StatusCode.FAIL, "只能更换同一校区的教练");
        }

        // 原错误逻辑：检查是否有非APPROVED状态的申请（包括REJECTED）
        // Optional<CoachChangeRequestEntity> existingRequest = changeRequestRepository
        //         .findByStudentIdAndStatusNot(studentId, CoachChangeRequestEntity.Status.APPROVED);

        // 新逻辑：仅检查是否有PENDING（待处理）状态的申请
        Optional<CoachChangeRequestEntity> existingPendingRequest = changeRequestRepository
                .findByStudentIdAndStatus(studentId, CoachChangeRequestEntity.Status.PENDING);
        if (existingPendingRequest.isPresent()) {
            return Result.error(StatusCode.FAIL, "您已有未完成的更换教练申请，请等待处理");
        }

        // 创建更换教练申请
        CoachChangeRequestEntity request = new CoachChangeRequestEntity();
        request.setStudentId(studentId);
        request.setCurrentCoachId(currentCoachId);
        request.setTargetCoachId(targetCoachId);
        request.setSchoolId(currentCoachOpt.get().getSchoolId());
        request.setStatus(CoachChangeRequestEntity.Status.PENDING);
        request.setCreateTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());

        CoachChangeRequestEntity savedRequest = changeRequestRepository.save(request);

        // 获取校区管理员ID
        Optional<SchoolEntity> schoolOpt = schoolRepository.findById(request.getSchoolId());
        if (schoolOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "校区信息不存在");
        }
        Long adminId = schoolOpt.get().getAdminId();

        // 发送通知给当前教练、目标教练和管理员
        notificationService.createCoachChangeRequestNotification(
                currentCoachId,
                NotificationEntity.UserType.COACH,
                savedRequest.getId(),
                String.format("学员%s申请更换教练，请处理", student.getName())
        );

        notificationService.createCoachChangeRequestNotification(
                targetCoachId,
                NotificationEntity.UserType.COACH,
                savedRequest.getId(),
                String.format("学员%s希望更换为您作为教练，请处理", student.getName())
        );

        notificationService.createCoachChangeRequestNotification(
                adminId,
                NotificationEntity.UserType.ADMIN,
                savedRequest.getId(),
                String.format("学员%s申请更换教练，需要您审批", student.getName())
        );

        return Result.success(savedRequest);
    }

    /**
     * 处理更换教练申请（当前教练/目标教练/管理员）
     */
    @Transactional
    public Result<CoachChangeRequestEntity> handleChangeRequest(
            Long requestId, Long handlerId, NotificationEntity.UserType handlerType, boolean approve) {

        Optional<CoachChangeRequestEntity> requestOpt = changeRequestRepository.findById(requestId);
        if (requestOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "更换教练申请不存在");
        }

        CoachChangeRequestEntity request = requestOpt.get();

        // 根据处理人类型更新审批状态
        if (handlerType == NotificationEntity.UserType.COACH) {
            // 教练处理：判断是当前教练还是目标教练
            if (handlerId.equals(request.getCurrentCoachId())) {
                request.setCurrentCoachApproval(approve);
            } else if (handlerId.equals(request.getTargetCoachId())) {
                request.setTargetCoachApproval(approve);
            } else {
                return Result.error(StatusCode.FAIL, "您无权处理此申请");
            }
        } else if (handlerType == NotificationEntity.UserType.ADMIN) {
            // 管理员处理
            request.setAdminApproval(approve);
        } else {
            return Result.error(StatusCode.FAIL, "无效的处理人类型");
        }

        // 检查是否有人拒绝
        if (!approve) {
            request.setStatus(CoachChangeRequestEntity.Status.REJECTED);
        } else {
            // 检查是否所有相关方都已同意
            if (request.getCurrentCoachApproval() != null &&
                    request.getTargetCoachApproval() != null &&
                    request.getAdminApproval() != null &&
                    request.getCurrentCoachApproval() &&
                    request.getTargetCoachApproval() &&
                    request.getAdminApproval()) {

                request.setStatus(CoachChangeRequestEntity.Status.APPROVED);
                // 执行更换教练操作
                replaceCoach(request);
            }
        }

        request.setUpdateTime(LocalDateTime.now());
        CoachChangeRequestEntity updatedRequest = changeRequestRepository.save(request);

        // 发送审批结果通知给相关人员
        notifyApprovalResult(updatedRequest, handlerId, approve);

        return Result.success(updatedRequest);
    }

    /**
     * 执行更换教练的实际操作
     */
    private void replaceCoach(CoachChangeRequestEntity request) {
        // 删除原教练与学员的关联
        coachStudentRepository.deleteByStudentIdAndCoachId(
                request.getStudentId(), request.getCurrentCoachId());

        // 创建新教练与学员的关联
        CoachTeachStudentEntity newRelation = new CoachTeachStudentEntity();
        newRelation.setCoachId(request.getTargetCoachId());
        newRelation.setStudentId(request.getStudentId());
        newRelation.setConfirmed(true);
        coachStudentRepository.save(newRelation);
    }

    /**
     * 发送审批结果通知
     */
    private void notifyApprovalResult(CoachChangeRequestEntity request, Long handlerId, boolean approve) {
        StudentEntity student = studentRepository.findById(request.getStudentId()).orElse(null);
        if (student == null) return;

        String studentName = student.getName();
        String result = approve ? "同意" : "拒绝";

        // 通知其他相关方
        if (!handlerId.equals(request.getCurrentCoachId()) && request.getCurrentCoachId() != null) {
            notificationService.createCoachChangeRequestNotification(
                    request.getCurrentCoachId(),
                    NotificationEntity.UserType.COACH,
                    request.getId(),
                    String.format("学员%s的更换教练申请已被%s，当前状态：%s",
                            studentName, result, request.getStatus())
            );
        }

        if (!handlerId.equals(request.getTargetCoachId()) && request.getTargetCoachId() != null) {
            notificationService.createCoachChangeRequestNotification(
                    request.getTargetCoachId(),
                    NotificationEntity.UserType.COACH,
                    request.getId(),
                    String.format("学员%s的更换教练申请已被%s，当前状态：%s",
                            studentName, result, request.getStatus())
            );
        }

        Optional<SchoolEntity> schoolOpt = schoolRepository.findById(request.getSchoolId());
        if (schoolOpt.isPresent() && !handlerId.equals(schoolOpt.get().getAdminId())) {
            notificationService.createCoachChangeRequestNotification(
                    schoolOpt.get().getAdminId(),
                    NotificationEntity.UserType.ADMIN,
                    request.getId(),
                    String.format("学员%s的更换教练申请已被%s，当前状态：%s",
                            studentName, result, request.getStatus())
            );
        }

        // 通知学员
        notificationService.createCoachChangeRequestNotification(
                request.getStudentId(),
                NotificationEntity.UserType.STUDENT,
                request.getId(),
                String.format("您的更换教练申请已被%s，当前状态：%s",
                        result, request.getStatus())
        );
    }

    /**
     * 获取用户相关的更换教练申请
     */
    public Result<List<CoachChangeRequestEntity>> getRelatedRequests(
            Long userId, NotificationEntity.UserType userType) {

        List<CoachChangeRequestEntity> requests;

        if (userType == NotificationEntity.UserType.STUDENT) {
            requests = changeRequestRepository.findByStudentId(userId);
        } else if (userType == NotificationEntity.UserType.COACH) {
            // 教练能看到作为当前教练或目标教练的申请
            List<CoachChangeRequestEntity> asCurrent = changeRequestRepository
                    .findByCurrentCoachIdAndStatus(userId, CoachChangeRequestEntity.Status.PENDING);
            List<CoachChangeRequestEntity> asTarget = changeRequestRepository
                    .findByTargetCoachIdAndStatus(userId, CoachChangeRequestEntity.Status.PENDING);
            requests = List.of(asCurrent, asTarget).stream().flatMap(List::stream).toList();
        } else if (userType == NotificationEntity.UserType.ADMIN) {
            // 管理员能看到其管理校区的申请
            List<SchoolEntity> managedSchools = schoolRepository.findByAdminId(userId);
            requests = managedSchools.stream()
                    .flatMap(school -> changeRequestRepository
                            .findBySchoolIdAndStatus(school.getId(), CoachChangeRequestEntity.Status.PENDING).stream())
                    .toList();
        } else {
            return Result.error(StatusCode.FAIL, "无效的用户类型");
        }

        return Result.success(requests);
    }
}