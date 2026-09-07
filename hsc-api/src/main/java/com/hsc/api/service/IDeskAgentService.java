package com.hsc.api.service;

/**
 * 座机坐席会话：签入/签出（含 FS 注册校验）与话机注册状态查询。
 *
 * <p>放 hsc-api 编排层而非 hsc-system：注册校验依赖 hsc-esl 的 ISipRegService（sofia reg
 * 实时查询），hsc-system 不能反向依赖 hsc-esl。状态写入复用 ISipAgentService.updateOnlineStatus，
 * 与软电话签入落在同一 Redis hash（AGENT_CURRENT_STATUS_KEY），外呼任务分配器对两种坐席无差别可见。
 */
public interface IDeskAgentService {

    /**
     * 座机签入：校验坐席绑定与话机 FS 注册，通过后写状态表（空闲）。
     *
     * @throws com.hsc.common.exception.CommonException 未绑定坐席 / 分机未配置 / 话机未注册
     */
    void signIn(Long userId);

    /**
     * 座机签出：状态置离线并从状态表移除（外呼任务分配不再可见；不影响呼入接听，转座机由 FS 直投分机）。
     */
    void signOut(Long userId);

    /**
     * 当前用户坐席话机是否已注册到 FS（座机点「拨打」前预检用）。
     *
     * @return true=已注册；未绑定坐席/未配置分机/未注册均返回 false
     */
    boolean isPhoneRegistered(Long userId);
}
