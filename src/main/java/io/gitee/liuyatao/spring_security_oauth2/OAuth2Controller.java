package io.gitee.liuyatao.spring_security_oauth2;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 api
 */
@Controller
@RequestMapping("/oauth2/api")
public class OAuth2Controller {

    @ResponseBody
    @RequestMapping(value = "/me")
    public Object test(Principal principal){
        return principal;
    }

}
