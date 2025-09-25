package org.example.pingpongsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.pingpongsystem.entity.*;
import org.example.pingpongsystem.repository.*;
import org.example.pingpongsystem.utility.Result;
import org.example.pingpongsystem.utility.StatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonthlyMatchService {

    private final MonthlyMatchRepository matchRepository;
    private final MatchRegistrationRepository registrationRepository;
    private final MatchGroupRepository groupRepository;
    private final MatchScheduleRepository scheduleRepository;
    private final StudentAccountRepository studentAccountRepository;
    private final PaymentService paymentService;
    private final TableRepository tableRepository;
    private final StudentRepository studentRepository;

    // 定时任务触发后创建比赛，比赛开始时间为触发后5分钟
    @Scheduled(cron = "0 19 13 23 9 ?") // 触发时间：2025年9月23日10:16
    @Transactional
    public void createMonthlyMatch() {
        // 获取定时任务触发时的当前时间（包含时分秒）
        LocalDateTime now = LocalDateTime.now();

        // 比赛开始时间 = 定时任务触发时间 + 5分钟
        LocalDateTime matchTime = now.plusMinutes(5);

        // 截止时间 = 比赛开始时间 - 1分钟
        LocalDateTime deadline = matchTime.minusMinutes(1);

        MonthlyMatchEntity match = new MonthlyMatchEntity();
        match.setTitle(now.getYear() + "年" + now.getMonthValue() + "月乒乓球月赛");
        match.setStartTime(matchTime); // 设置比赛开始时间
        match.setRegistrationDeadline(deadline); // 设置截止时间
        match.setYear(now.getYear());
        match.setMonth(now.getMonthValue());
        match.setStatus(MonthlyMatchEntity.MatchStatus.NOT_STARTED);

        matchRepository.save(match);
    }

    // 每月1日自动创建当月比赛
//    @Scheduled(cron = "0 0 0 1 * ?")
//    @Transactional
//    public void createMonthlyMatch() {
//        LocalDate now = LocalDate.now();
//        // 找到当月第四个星期天
//        LocalDate fourthSunday = now.with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.SUNDAY));
//        LocalDateTime matchTime = fourthSunday.atTime(9, 0); // 上午9点开始
//        LocalDateTime deadline = matchTime.minusWeeks(1); // 提前7天截止
//
//        MonthlyMatchEntity match = new MonthlyMatchEntity();
//        match.setTitle(now.getYear() + "年" + now.getMonthValue() + "月乒乓球月赛");
//        match.setStartTime(matchTime);
//        match.setRegistrationDeadline(deadline);
//        match.setYear(now.getYear());
//        match.setMonth(now.getMonthValue());
//        match.setStatus(MonthlyMatchEntity.MatchStatus.NOT_STARTED);
//
//        matchRepository.save(match);
//    }

    // 管理员修改比赛时间
    @Transactional
    public Result<MonthlyMatchEntity> updateMatchTime(Long matchId, LocalDateTime startTime) {
        Optional<MonthlyMatchEntity> matchOpt = matchRepository.findById(matchId);
        if (matchOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "月赛不存在");
        }

        MonthlyMatchEntity match = matchOpt.get();
        // 确保修改时间在报名开始前
        if (match.getStatus() != MonthlyMatchEntity.MatchStatus.NOT_STARTED) {
            return Result.error(StatusCode.FAIL, "只能在报名开始前修改比赛时间");
        }

        match.setStartTime(startTime);
        match.setRegistrationDeadline(startTime.minusWeeks(1));
        matchRepository.save(match);
        return Result.success(match);
    }

    // 学员报名月赛
    @Transactional
    public Result<MatchRegistrationEntity> registerForMatch(Long matchId, Long studentId, MatchRegistrationEntity.GroupType groupType) {
        Optional<MonthlyMatchEntity> matchOpt = matchRepository.findById(matchId);
        if (matchOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "月赛不存在");
        }

        MonthlyMatchEntity match = matchOpt.get();
        // 检查报名时间是否有效
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(match.getStartTime().minusMonths(1)) || now.isAfter(match.getRegistrationDeadline())) {
            return Result.error(StatusCode.FAIL, "不在报名时间范围内");
        }

        // 检查是否已报名
        Optional<MatchRegistrationEntity> existingReg = registrationRepository.findByMonthlyMatchIdAndStudentId(matchId, studentId);
        if (existingReg.isPresent()) {
            return Result.error(StatusCode.FAIL, "您已报名该月赛");
        }

        // 检查余额
        Optional<StudentAccountEntity> accountOpt = studentAccountRepository.findByStudentId(studentId);
        if (accountOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "学员账户不存在");
        }

        StudentAccountEntity account = accountOpt.get();
        double fee = 30.0; // 报名费30元
        if (account.getBalance() < fee) {
            return Result.error(StatusCode.FAIL, "余额不足，请先充值");
        }

        // 创建支付记录并扣减余额
        PaymentRecordEntity paymentRecord = paymentService.createMatchPaymentRecord(studentId, fee);
        account.setBalance(account.getBalance() - fee);
        studentAccountRepository.save(account);

        // 创建报名记录
        MatchRegistrationEntity registration = new MatchRegistrationEntity();
        registration.setMonthlyMatchId(matchId);
        registration.setStudentId(studentId);
        registration.setGroupType(groupType);
        registration.setPaid(true);
        registration.setPaymentRecordId(paymentRecord.getId());
        registration.setRegistrationTime(LocalDateTime.now());

        MatchRegistrationEntity savedReg = registrationRepository.save(registration);
        return Result.success(savedReg);
    }

    // 定时任务：报名截止后自动安排赛程
    //@Scheduled(cron = "0 0 0 * * ?") // 每天凌晨执行
    @Scheduled(cron = "0 */1 * * * ?")  // 每1分钟执行一次（测试用，可调整为每小时）
    @Transactional
    public void arrangeMatchesAutomatically() {
        LocalDateTime now = LocalDateTime.now();
        // 找到所有已截止报名但未安排赛程的比赛
        List<MonthlyMatchEntity> matches = matchRepository.findAll().stream()
                .filter(match -> match.getRegistrationDeadline().isBefore(now) &&
                        match.getStatus() == MonthlyMatchEntity.MatchStatus.REGISTERING ||
                        match.getStatus() == MonthlyMatchEntity.MatchStatus.REGISTRATION_CLOSED &&
                                scheduleRepository.countByMonthlyMatchId(match.getId()) == 0)
                .collect(Collectors.toList());

        for (MonthlyMatchEntity match : matches) {
            arrangeMatchSchedule(match.getId());
            match.setStatus(MonthlyMatchEntity.MatchStatus.REGISTRATION_CLOSED);
            matchRepository.save(match);
        }
    }


    // 获取学员的报名记录
    public Result<List<MatchRegistrationEntity>> getStudentRegistrations(Long studentId) {
        return Result.success(registrationRepository.findByStudentId(studentId));
    }

    // 获取月赛的所有组别报名情况
    public Result<Map<MatchRegistrationEntity.GroupType, Long>> getGroupRegistrationCounts(Long matchId) {
        Map<MatchRegistrationEntity.GroupType, Long> counts = new EnumMap<>(MatchRegistrationEntity.GroupType.class);
        for (MatchRegistrationEntity.GroupType groupType : MatchRegistrationEntity.GroupType.values()) {
            counts.put(groupType, registrationRepository.countByMonthlyMatchIdAndGroupType(matchId, groupType));
        }
        return Result.success(counts);
    }

    public Result<List<MatchScheduleEntity>> getStudentMatchSchedule(Long matchId, Long studentId) {
        // 1. 先校验月赛是否存在
        Optional<MonthlyMatchEntity> matchOpt = matchRepository.findById(matchId);
        if (matchOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "月赛不存在");
        }

        MonthlyMatchEntity match = matchOpt.get();
        // 2. 获取月赛当前状态（核心：基于MatchStatus枚举判断）
        MonthlyMatchEntity.MatchStatus matchStatus = match.getStatus(); // 假设MonthlyMatchEntity已存在status字段，类型为MatchStatus

        // 3. 仅当状态为【REGISTRATION_CLOSED（报名截止）】时，允许查看赛程
        if (matchStatus != MonthlyMatchEntity.MatchStatus.REGISTRATION_CLOSED) {
            // 根据不同状态返回精准提示（避免统一提示“报名尚未截止”）
            String errorMsg = switch (matchStatus) {
                case NOT_STARTED -> "比赛尚未开始报名，暂不能查看赛程";
                case REGISTERING -> "报名进行中，暂不能查看赛程（报名截止后开放）";
                case ONGOING -> "比赛已开始，可正常查看赛程"; // 若需要开放“进行中”状态查看，保留此分支；否则改为提示
                case COMPLETED -> "比赛已结束，可查看历史赛程"; // 若需要开放“已结束”状态查看，保留此分支；否则改为提示
                default -> "月赛状态异常，暂不能查看赛程"; // 兜底：未知状态
            };
            // 注意：若仅允许“REGISTRATION_CLOSED”查看，可将ONGOING/COMPLETED分支也改为错误提示，示例：
            // case ONGOING -> "比赛进行中，暂不支持查看赛程";
            // case COMPLETED -> "比赛已结束，暂不支持查看赛程";
            return Result.error(StatusCode.FAIL, errorMsg);
        }

        // 4. 状态符合条件：查询该学员作为选手1或选手2的所有赛程
        List<MatchScheduleEntity> schedules = scheduleRepository
                .findByMonthlyMatchIdAndPlayer1IdOrMonthlyMatchIdAndPlayer2Id(matchId, studentId, matchId, studentId);

        return Result.success(schedules);
    }

    // 获取当前可报名的月赛
    public Result<List<MonthlyMatchEntity>> getAvailableMatches() {
        LocalDateTime now = LocalDateTime.now();
        List<MonthlyMatchEntity> matches = matchRepository.findAll().stream()
                .filter(match -> now.isAfter(match.getStartTime().minusMonths(1)) &&
                        now.isBefore(match.getRegistrationDeadline()) &&
                        match.getStatus() == MonthlyMatchEntity.MatchStatus.REGISTERING)
                .collect(Collectors.toList());
        return Result.success(matches);
    }

    /**
     * 定时任务：检查未开始的比赛，自动切换到报名中（或直接截止并安排赛程）
     * 执行频率：每1分钟（测试用，正式环境可改为每10分钟/每小时，根据需求调整）
     */
    @Scheduled(cron = "0 */1 * * * ?")
    @Transactional
    public void checkAndSwitchMatchStatus() {
        LocalDateTime now = LocalDateTime.now();
        // 1. 查询所有“未开始”状态的比赛
        List<MonthlyMatchEntity> notStartedMatches = matchRepository.findByStatus(MonthlyMatchEntity.MatchStatus.NOT_STARTED);

        for (MonthlyMatchEntity match : notStartedMatches) {
            // 计算“报名开始时间”（原代码 registerForMatch 中定义为“比赛开始前1个月”）
            LocalDateTime registrationStartTime = match.getStartTime().minusMonths(1);
            // 报名截止时间（已在创建比赛时设置）
            LocalDateTime registrationDeadline = match.getRegistrationDeadline();

            // 2. 状态流转判断
            if (now.isAfter(registrationStartTime) || now.isEqual(registrationStartTime)) {
                // 2.1 若当前时间在“报名开始后 + 截止前”：切换为【报名中】
                if (now.isBefore(registrationDeadline)) {
                    match.setStatus(MonthlyMatchEntity.MatchStatus.REGISTERING);
                    matchRepository.save(match);
                }
                // 2.2 若当前时间已过“报名截止时间”：直接切换为【报名截止】并安排赛程
                else if (now.isAfter(registrationDeadline) || now.isEqual(registrationDeadline)) {
                    match.setStatus(MonthlyMatchEntity.MatchStatus.REGISTRATION_CLOSED);
                    matchRepository.save(match);
                    // 立即安排赛程（避免遗漏）
                    arrangeMatchSchedule(match.getId());
                }
            }
        }
    }

    /**
     * 根据ID查询单个月赛详情（用于前端报名记录关联月赛信息）
     */
    public Result<MonthlyMatchEntity> getMatchById(Long matchId) {
        Optional<MonthlyMatchEntity> matchOpt = matchRepository.findById(matchId);
        if (matchOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "月赛不存在");
        }
        return Result.success(matchOpt.get());
    }


    /**
     * 1. 顶层赛程安排入口：按组别拆分报名者，创建小组并触发组内全循环赛程
     * 核心修改：确保分组逻辑正确，传递必要参数给子函数
     */
    private void arrangeMatchSchedule(Long matchId) {
        // 遍历所有组别类型（如男子组、女子组等，根据GroupType枚举动态适配）
        for (MatchRegistrationEntity.GroupType groupType : MatchRegistrationEntity.GroupType.values()) {
            // 1. 获取当前月赛、当前组别的有效报名记录
            List<MatchRegistrationEntity> registrations = registrationRepository
                    .findByMonthlyMatchIdAndGroupType(matchId, groupType);
            if (registrations.isEmpty()) {
                continue; // 该组别无报名，跳过处理
            }

            // 2. 获取所有可用球台（无球台时抛异常，确保赛程可执行）
            List<TableEntity> availableTables = tableRepository.findAll();
            if (availableTables.isEmpty()) {
                throw new RuntimeException("月赛ID：" + matchId + "，组别：" + groupType + " - 无可用球台，无法安排比赛");
            }

            // 3. 将报名者拆分为「每组最多6人」的子组（超过6人时分更小的组）
            List<List<MatchRegistrationEntity>> subgroups = splitIntoSubgroups(registrations);

            // 4. 为每个子组创建小组记录，并安排组内全循环比赛
            for (int i = 0; i < subgroups.size(); i++) {
                List<MatchRegistrationEntity> currentSubgroup = subgroups.get(i);

                // 4.1 保存小组信息（关联月赛、组别、小组编号、实际人数）
                MatchGroupEntity group = new MatchGroupEntity();
                group.setMonthlyMatchId(matchId);
                group.setGroupType(groupType);
                group.setSubgroupNumber(i + 1); // 小组编号从1开始（用户易理解）
                group.setSize(currentSubgroup.size()); // 记录真实参赛人数（不含虚拟轮空）
                MatchGroupEntity savedGroup = groupRepository.save(group);

                // 4.2 触发当前子组的全循环赛程安排
                arrangeSubgroupMatches(savedGroup.getId(), currentSubgroup, availableTables);
            }
        }
    }

    /**
     * 2. 报名者分组：将报名列表拆分为「每组最多6人」的子组，支持随机打乱
     * 核心修改：将subList转为独立ArrayList，避免原列表修改导致的视图异常
     */
    private List<List<MatchRegistrationEntity>> splitIntoSubgroups(List<MatchRegistrationEntity> registrations) {
        List<List<MatchRegistrationEntity>> subgroups = new ArrayList<>();
        final int MAX_GROUP_SIZE = 6; // 每组最多6人（需求不变）
        final int MIN_GROUP_SIZE = 2; // 每组最少2人（解决1人组问题）
        int totalRegistrants = registrations.size();

        // 前置过滤：不足2人无法比赛，直接返回空列表（不生成小组）
        if (totalRegistrants < MIN_GROUP_SIZE) {
            return subgroups;
        }

        // 步骤1：计算总组数（向上取整，确保每组≤MAX_GROUP_SIZE）
        int groupCount;
        if (totalRegistrants <= MAX_GROUP_SIZE) {
            groupCount = 1; // 人数≤6时，1组即可
        } else {
            // 向上取整公式：(总数 + 每组最大数 - 1) / 每组最大数（避免使用Math.ceil，兼容整数运算）
            groupCount = (totalRegistrants + MAX_GROUP_SIZE - 1) / MAX_GROUP_SIZE;
        }

        // 步骤2：计算每组基础人数和余数（分配余数确保每组≥MIN_GROUP_SIZE）
        int baseSize = totalRegistrants / groupCount; // 基础人数（每组至少baseSize人）
        int remainder = totalRegistrants % groupCount; // 余数（前remainder组需多1人）

        // 步骤3：随机打乱报名顺序（公平性保障，保留原逻辑）
        Collections.shuffle(registrations);

        // 步骤4：拆分分组（前remainder组为baseSize+1人，剩余为baseSize人）
        int currentIndex = 0; // 当前拆分的起始索引
        for (int i = 0; i < groupCount; i++) {
            // 计算当前组的人数：前remainder组多1人
            int currentGroupSize = (i < remainder) ? (baseSize + 1) : baseSize;
            // 计算当前组的结束索引（避免超出列表长度）
            int endIndex = Math.min(currentIndex + currentGroupSize, totalRegistrants);
            // 截取子列表并转为独立ArrayList（避免原列表修改导致的视图异常）
            List<MatchRegistrationEntity> subgroup = new ArrayList<>(registrations.subList(currentIndex, endIndex));
            subgroups.add(subgroup);
            // 更新下一组的起始索引
            currentIndex = endIndex;
        }

        return subgroups;
    }

    /**
     * 3. 单场比赛创建：封装比赛基本信息，关联小组、轮次、选手、球台
     * 核心修改：增强异常提示，明确参数来源，确保轮空时player2Id为null
     */
    private MatchScheduleEntity createMatchSchedule(Long groupId, int round, Long player1Id, Long player2Id, List<TableEntity> tables) {
        // 通过小组ID查询关联的月赛ID（确保数据一致性）
        MatchGroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("小组不存在，小组ID：" + groupId));

        MatchScheduleEntity schedule = new MatchScheduleEntity();
        schedule.setMonthlyMatchId(group.getMonthlyMatchId()); // 关联月赛（从小组间接获取，避免直接传参错误）
        schedule.setGroupId(groupId); // 关联当前小组
        schedule.setRoundNumber(round); // 轮次（从循环参数传入）
        schedule.setPlayer1Id(player1Id); // 选手1（轮空时不为null）
        schedule.setPlayer2Id(player2Id); // 选手2（轮空时为null，符合实体定义）
        // 随机分配球台：从可用球台中随机选一个（确保球台资源有效）
        schedule.setTableId(tables.get(new Random().nextInt(tables.size())).getId());
        schedule.setResult(MatchScheduleEntity.MatchResult.NOT_STARTED); // 初始状态：未开始

        return schedule;
    }

    /**
     * 4. 组内全循环赛程：核心逻辑，实现「固定1号+顺时针轮转」+ 奇数轮空优化
     * 核心修改：重构对阵逻辑，用虚拟轮空统一奇偶处理，确保匹配用户示例
     */
    private void arrangeSubgroupMatches(Long groupId, List<MatchRegistrationEntity> subgroup, List<TableEntity> tables) {
        // 步骤1：提取当前小组的真实选手ID（排除轮空，仅保留实际报名学员）
        List<Long> realPlayerIds = subgroup.stream()
                .map(MatchRegistrationEntity::getStudentId)
                .collect(Collectors.toList());
        int realPlayerCount = realPlayerIds.size();
        if (realPlayerCount < 2) {
            return; // 不足2人，无法安排比赛（含轮空也至少需2个"名额"）
        }

        // 步骤2：奇数人数处理：添加虚拟轮空标记（-1L），统一按偶数逻辑计算
        List<Long> extendedPlayerIds = new ArrayList<>(realPlayerIds);
        if (realPlayerCount % 2 != 0) {
            extendedPlayerIds.add(-1L); // -1L为虚拟轮空，后续替换为null
        }
        int extendedSize = extendedPlayerIds.size(); // 扩展后的人数（必为偶数）
        int totalRounds = extendedSize - 1; // 全循环轮次：人数-1（如6人5轮，5人+轮空6人也5轮）

        // 步骤3：初始化当前轮选手列表（从初始扩展列表开始）
        List<Long> currentRoundPlayers = new ArrayList<>(extendedPlayerIds);

        // 步骤4：循环安排每一轮比赛
        for (int round = 1; round <= totalRounds; round++) {
            List<MatchScheduleEntity> roundMatches = new ArrayList<>();

            // 4.1 生成当前轮对阵：从列表两端向中间配对（0↔n-1，1↔n-2，...）
            for (int i = 0; i < extendedSize / 2; i++) {
                int opponentIndex = extendedSize - 1 - i;
                Long player1 = currentRoundPlayers.get(i);
                Long player2 = currentRoundPlayers.get(opponentIndex);

                // 4.2 虚拟轮空替换：将-1L转为null（符合MatchScheduleEntity的轮空定义）
                player1 = (player1 != null && player1.equals(-1L)) ? null : player1;
                player2 = (player2 != null && player2.equals(-1L)) ? null : player2;

                // 4.3 轮空逻辑优化：确保player1不为null（轮空选手作为player1，player2为null）
                if (player1 == null) {
                    // 交换选手顺序，避免player1为null（实体中player1可非空，player2可空）
                    Long temp = player1;
                    player1 = player2;
                    player2 = temp;
                }

                // 4.4 无效配对过滤（理论上不会触发，双重保障）
                if (player1 == null) {
                    continue;
                }

                // 4.5 创建单场比赛并加入当前轮列表
                MatchScheduleEntity match = createMatchSchedule(
                        groupId, round, player1, player2, tables);
                roundMatches.add(match);
            }

            // 4.6 保存当前轮比赛（批量保存，提升效率）
            if (!roundMatches.isEmpty()) {
                scheduleRepository.saveAll(roundMatches);
            }

            // 4.7 顺时针轮转：固定第1位，将最后1位移到第2位（核心轮转逻辑）
            if (extendedSize > 2) { // 仅当人数>2时需要轮转（人数=2时1轮即可，无需轮转）
                Long lastPlayer = currentRoundPlayers.remove(extendedSize - 1); // 移除最后一位
                currentRoundPlayers.add(1, lastPlayer); // 插入到第2位（索引1）
            }
        }
    }

    /**
     * 管理员创建新比赛
     */
    @Transactional
    public Result<MonthlyMatchEntity> createMatchByAdmin(String title, LocalDateTime startTime, LocalDateTime registrationDeadline) {
        // 验证时间逻辑：截止时间必须在开始时间之前
        if (registrationDeadline.isAfter(startTime)) {
            return Result.error(StatusCode.FAIL, "报名截止时间不能晚于比赛开始时间");
        }

        // 验证时间逻辑：不能创建过去的比赛
        if (startTime.isBefore(LocalDateTime.now())) {
            return Result.error(StatusCode.FAIL, "不能创建过去的比赛");
        }

        MonthlyMatchEntity match = new MonthlyMatchEntity();
        match.setTitle(title);
        match.setStartTime(startTime);
        match.setRegistrationDeadline(registrationDeadline);
        match.setYear(startTime.getYear());
        match.setMonth(startTime.getMonthValue());

        // 根据当前时间和截止时间设置初始状态
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(registrationDeadline) && now.isBefore(startTime.minusMonths(1))) {
            match.setStatus(MonthlyMatchEntity.MatchStatus.NOT_STARTED);
        } else if (now.isAfter(startTime.minusMonths(1)) && now.isBefore(registrationDeadline)) {
            match.setStatus(MonthlyMatchEntity.MatchStatus.REGISTERING);
        } else {
            match.setStatus(MonthlyMatchEntity.MatchStatus.REGISTRATION_CLOSED);
        }

        MonthlyMatchEntity savedMatch = matchRepository.save(match);
        return Result.success(savedMatch);
    }

    /**
     * 管理员更新比赛信息
     */
    @Transactional
    public Result<MonthlyMatchEntity> updateMatchByAdmin(
            Long matchId,
            String title,
            LocalDateTime startTime,
            LocalDateTime registrationDeadline,
            MonthlyMatchEntity.MatchStatus status) {

        Optional<MonthlyMatchEntity> matchOpt = matchRepository.findById(matchId);
        if (matchOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "月赛不存在");
        }

        MonthlyMatchEntity match = matchOpt.get();
        LocalDateTime now = LocalDateTime.now();

        // 保存原始截止时间用于状态校验（如果更新了截止时间则用新值）
        LocalDateTime effectiveDeadline = registrationDeadline != null ? registrationDeadline : match.getRegistrationDeadline();

        // 更新标题
        if (title != null && !title.isEmpty()) {
            match.setTitle(title);
        }

        // 更新时间
        if (startTime != null) {
            // 如果同时更新了开始时间和截止时间
            if (registrationDeadline != null) {
                if (registrationDeadline.isAfter(startTime)) {
                    return Result.error(StatusCode.FAIL, "报名截止时间不能晚于比赛开始时间");
                }
                match.setRegistrationDeadline(registrationDeadline);
            } else {
                // 只更新开始时间，保持原截止时间与开始时间的相对关系
                long daysBetween = ChronoUnit.DAYS.between(match.getStartTime(), match.getRegistrationDeadline());
                match.setRegistrationDeadline(startTime.plusDays(daysBetween));
                effectiveDeadline = match.getRegistrationDeadline(); // 更新有效截止时间
            }
            match.setStartTime(startTime);
            match.setYear(startTime.getYear());
            match.setMonth(startTime.getMonthValue());
        } else if (registrationDeadline != null) {
            // 只更新截止时间
            if (registrationDeadline.isAfter(match.getStartTime())) {
                return Result.error(StatusCode.FAIL, "报名截止时间不能晚于比赛开始时间");
            }
            match.setRegistrationDeadline(registrationDeadline);
            effectiveDeadline = registrationDeadline; // 更新有效截止时间
        }

        // 更新状态
        if (status != null) {
            // 新增限制：只有未到报名截止时间，才能设置为未开始或报名中
            if ((status == MonthlyMatchEntity.MatchStatus.NOT_STARTED ||
                    status == MonthlyMatchEntity.MatchStatus.REGISTERING) &&
                    now.isAfter(effectiveDeadline)) {
                return Result.error(StatusCode.FAIL, "已过报名截止时间，不能设置为" +
                        (status == MonthlyMatchEntity.MatchStatus.NOT_STARTED ? "未开始" : "报名中"));
            }

            // 原有状态变更限制条件
            if (status == MonthlyMatchEntity.MatchStatus.COMPLETED &&
                    match.getStatus() != MonthlyMatchEntity.MatchStatus.ONGOING) {
                return Result.error(StatusCode.FAIL, "只有进行中的比赛才能标记为已完成");
            }

            if (status == MonthlyMatchEntity.MatchStatus.ONGOING &&
                    (match.getStatus() == MonthlyMatchEntity.MatchStatus.NOT_STARTED ||
                            match.getStatus() == MonthlyMatchEntity.MatchStatus.COMPLETED)) {
                return Result.error(StatusCode.FAIL, "只有报名截止的比赛才能标记为进行中");
            }

            match.setStatus(status);
        }

        MonthlyMatchEntity updatedMatch = matchRepository.save(match);
        return Result.success(updatedMatch);
    }


    /**
     * 获取所有比赛，支持按年和月筛选
     */
    public Result<List<MonthlyMatchEntity>> getAllMatches(Integer year, Integer month) {
        List<MonthlyMatchEntity> matches;

        if (year != null && month != null) {
            matches = matchRepository.findByYearAndMonth(year, month);
        } else if (year != null) {
            matches = matchRepository.findByYear(year);
        } else {
            matches = matchRepository.findAll();
            // 按时间倒序排序
            matches.sort((m1, m2) -> m2.getStartTime().compareTo(m1.getStartTime()));
        }

        // 2. 新增：为每条比赛的 hasSchedule 赋值
        matches.forEach(match -> {
            long scheduleCount = scheduleRepository.countByMonthlyMatchId(match.getId());
            match.setHasSchedule(scheduleCount > 0);
        });

        return Result.success(matches);
    }

    /**
     * 管理员手动触发赛程安排
     */
    @Transactional
    public Result<Void> manuallyArrangeSchedule(Long matchId) {
        Optional<MonthlyMatchEntity> matchOpt = matchRepository.findById(matchId);
        if (matchOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "月赛不存在");
        }

        MonthlyMatchEntity match = matchOpt.get();

        // 新增：检查是否已安排过赛程
        long existingScheduleCount = scheduleRepository.countByMonthlyMatchId(matchId);
        if (existingScheduleCount > 0) {
            return Result.error(StatusCode.FAIL, "该比赛已安排过赛程，不可重复安排");
        }

        // 检查比赛状态是否允许安排赛程
        if (match.getStatus() != MonthlyMatchEntity.MatchStatus.REGISTERING &&
                match.getStatus() != MonthlyMatchEntity.MatchStatus.REGISTRATION_CLOSED) {
            return Result.error(StatusCode.FAIL, "只有报名中或报名截止的比赛才能安排赛程");
        }

        // 检查是否已过截止时间
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(match.getRegistrationDeadline()) &&
                match.getStatus() == MonthlyMatchEntity.MatchStatus.REGISTERING) {
            // 如果尚未截止，但管理员强制安排，需要先关闭报名
            match.setStatus(MonthlyMatchEntity.MatchStatus.REGISTRATION_CLOSED);
            matchRepository.save(match);
        }

        // 安排赛程
        arrangeMatchSchedule(matchId);
        return Result.success();
    }

    /**
     * 管理员获取某比赛的全量赛程（关联小组信息+选手姓名）
     */
    public Result<List<MatchScheduleEntity>> getAdminMatchSchedule(Long matchId) {
        // 1. 校验月赛是否存在
        Optional<MonthlyMatchEntity> matchOpt = matchRepository.findById(matchId);
        if (matchOpt.isEmpty()) {
            return Result.error(StatusCode.FAIL, "月赛不存在");
        }

        // 2. 查询该比赛的所有赛程
        List<MatchScheduleEntity> schedules = scheduleRepository.findByMonthlyMatchId(matchId);

        // 3. 关联小组信息（组别类型+子组号，如「GROUP_A-第1组」）
        // 关联选手姓名（若有学生表，无则可注释）
        for (MatchScheduleEntity schedule : schedules) {
            // 3.1 关联小组信息（从MatchGroupEntity获取）
            Optional<MatchGroupEntity> groupOpt = groupRepository.findById(schedule.getGroupId());
            groupOpt.ifPresent(group -> {
                // 在MatchScheduleEntity中新增2个「非持久化字段」存储小组信息（见步骤3）
                schedule.setGroupTypeStr(group.getGroupType().name()); // 如 GROUP_A
                schedule.setSubgroupNumber(group.getSubgroupNumber()); // 如 1
            });

            // 3.2 关联选手姓名（从StudentEntity获取，无学生表可删除）
            // 选手1姓名
            if (schedule.getPlayer1Id() != null) {
                Optional<StudentEntity> student1Opt = studentRepository.findById(schedule.getPlayer1Id());
                student1Opt.ifPresent(student -> schedule.setPlayer1Name(student.getUsername() + "(" + student.getName() + ")"));
            }
            // 选手2姓名（轮空时为null）
            if (schedule.getPlayer2Id() != null) {
                Optional<StudentEntity> student2Opt = studentRepository.findById(schedule.getPlayer2Id());
                student2Opt.ifPresent(student -> schedule.setPlayer2Name(student.getUsername() + "(" + student.getName() + ")"));
            }
        }

        return Result.success(schedules);
    }
}
