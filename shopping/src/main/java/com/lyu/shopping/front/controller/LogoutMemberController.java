package com.lyu.shopping.front.controller;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

/**
 * 类描述：处理商城会员退出操作的控制器
 * 类名称：com.lyu.shopping.sysmanage.controller.LogoutMemberController
 * @author 曲健磊
 * 2018年4月17日.下午7:01:29
 * @version V1.0
 */
@Controller
public class LogoutMemberController {
	
	/**
	 * 用来打印日志
	 */
	Logger logger = Logger.getLogger(LogoutMemberController.class);

	/**
	 * 商城用户登录页面的URI
	 */
	public static final String LOGIN_MEMBER_URI = "loginMember";

	/**
	 * 处理退出后台管理系统的请求
	 * @return
	 */
	@RequestMapping(value="/quit")
	public String gotoLoginMember(HttpSession session) {
		session.removeAttribute("member");
		return LOGIN_MEMBER_URI;
	}
}
