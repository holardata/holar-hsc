package com.hsc.system;

import cn.hutool.core.util.IdUtil;
import com.hsc.ai.service.IDashScopeService;
import com.hsc.api.HscApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

@SpringBootTest(classes = HscApiApplication.class)
class SystemApplicationTests {

    @Autowired
    private ApplicationEventPublisher publisher;

    /*@Autowired
    private IDeepSeekService deepSeekService;*/

    @Autowired
    private IDashScopeService dashScopeService;

    @Test
    void contextLoads() throws InterruptedException {
        String chatId = IdUtil.fastSimpleUUID();
        String chat = dashScopeService.chat(chatId, "你好，介绍下你自己！");
        System.out.println(chat);
        String str = dashScopeService.chat(chatId, "帮我写首登高的五言绝句");
        System.out.println(str);
        String str1 = dashScopeService.chat(chatId, "复述上述问题");
        System.out.println(str1);
        String str2 = dashScopeService.chat(chatId, "是的");
        System.out.println(str2);
    }


}
