package com.yqrb.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @ClassName $ {NAME}
 * @Description TODO
 * @Author fjw
 * @Date 2020/2/16 9:18 PM
 * @Version 1.0
 **/
@ApiIgnore
@RestController
public class HelloController {
    //final static Logger logger = LoggerFactory.getLogger(HelloController.class);

    @GetMapping("/hello")
    public Object hello() {
//        logger.debug("debug: hello");
//        logger.info("info: hello");
//        logger.warn("warn: hello");
//        logger.error("error: hello");
        return "Hello World~";
    }

    @GetMapping("/setSession")
    public Object setSession(HttpServletRequest request){
        HttpSession session = request.getSession();
        session.setAttribute("userInfo","new user");
        session.setMaxInactiveInterval(3600);
        session.getAttribute("userInfo");
        session.removeAttribute("userInfo");
        return "ok";
    }

}
