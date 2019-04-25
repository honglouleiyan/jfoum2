/*
 * Copyright (c) JForum Team
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above 
 * copyright notice, this list of conditions and the 
 * following disclaimer.
 * 2) Redistributions in binary form must reproduce the 
 * above copyright notice, this list of conditions and 
 * the following disclaimer in the documentation and/or 
 * other materials provided with the distribution.
 * 3) Neither the name of "Rafael Steil" nor 
 * the names of its contributors may be used to endorse 
 * or promote products derived from this software without 
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT 
 * HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, 
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 * 
 * Created on Jan 3, 2005 1:20:24 PM
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.sso;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import net.jforum.JForumExecutionContext;
import net.jforum.dao.UserDAO;
import net.jforum.entities.User;
import net.jforum.exceptions.ForumException;
import net.jforum.util.DbUtils;
import net.jforum.util.Hash;
import net.jforum.util.aqara.*;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.view.forum.UserAction;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * Default login authenticator for JForum.
 * This authenticator will validate the input against
 * <i>jforum_users</i>. 
 * 
 * @author Rafael Steil
 * @version $Id$
 */
public class DefaultLoginAuthenticator implements LoginAuthenticator
{

    private static final Logger LOGGER = Logger.getLogger(DefaultLoginAuthenticator.class);
	private transient UserDAO userModel;


	/**
	 * @see net.jforum.sso.LoginAuthenticator#setUserModel(net.jforum.dao.UserDAO)
	 */
	@Override
	public void setUserModel(final UserDAO userModel)
	{
		this.userModel = userModel;
	}

	/**
	 * @see net.jforum.sso.LoginAuthenticator#validateLogin(String, String, java.util.Map) 
	 */
	@Override
	public User validateLogin(final String username, final String password, final Map<?, ?> extraParams)
	{
		User user = null;
		ResultSet resultSet = null;
		PreparedStatement pstmt = null;
		
		try 
		{

			if(!"Admin".equalsIgnoreCase(username)) {
				int userid = 0;
				Map<String, Object> params = new HashMap<>(2);
				params.put("account", username);
				params.put("password", Md5Util.MD5_32(password));
				Map<String, String> headerMap = HttpCommonRequest.getHeaderMap(new HttpCommomHeader());
				String retStr = PooledHttpClientUtils.doPost("http://110.43.34.124/saas/v1.0" + "/lumi/user/login", headerMap, params);
				LOGGER.info("retStr:" + retStr);
				if (StringUtils.isNotBlank(retStr)) {
					ResponseMsg responseMsg = JSONObject.toJavaObject(JSONObject.parseObject(retStr), ResponseMsg.class);
					if (responseMsg != null && responseMsg.getCode() == 0) {
						JSONObject resultJson = (JSONObject) responseMsg.getResult();
						String userId = resultJson.getString("userId");
						if (StringUtils.isNotBlank(userId)) {
							User newUser = this.userModel.findByEmail(userId);
							if (newUser == null) {
								//新用户   同步如数据库
								newUser = new User();
								newUser.setUsername(username);
								newUser.setEmail(userId);
								newUser.setPassword(password);
								userid = this.userModel.addNew(newUser);

							} else {
								return newUser;
							}


						}
					}

				}
				if (userid != 0) {
					user = this.userModel.selectById(userid);
				} else {
					return null;
				}
			} else {


				pstmt = JForumExecutionContext.getConnection().prepareStatement(
						SystemGlobals.getSql("UserModel.login"));
				pstmt.setString(1, username);
				// first try MD5 hash
				pstmt.setString(2, Hash.md5(password));

				resultSet = pstmt.executeQuery();
				if (resultSet.next() && resultSet.getInt("user_id") > 0) {
					user = this.userModel.selectById(resultSet.getInt("user_id"));
				} else {
					resultSet.close();
					// then, SHA-512
					pstmt.setString(2, Hash.sha512(password));

					resultSet = pstmt.executeQuery();
					if (resultSet.next() && resultSet.getInt("user_id") > 0) {
						user = this.userModel.selectById(resultSet.getInt("user_id"));
					} else {
						resultSet.close();
						// then, SHA-512 with a salt
						String pwd = Hash.sha512(password + SystemGlobals.getValue(ConfigKeys.USER_HASH_SEQUENCE));
						pstmt.setString(2, Hash.sha512(password + SystemGlobals.getValue(ConfigKeys.USER_HASH_SEQUENCE)));

						resultSet = pstmt.executeQuery();
						if (resultSet.next() && resultSet.getInt("user_id") > 0) {
							user = this.userModel.selectById(resultSet.getInt("user_id"));
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			throw new ForumException(e);
		}
		finally
		{
			DbUtils.close(resultSet, pstmt);
		}

		if (user != null && !user.isDeleted() && (user.getActivationKey() == null || user.isActive())) {
			return user;
		}

		return null;
	}
}
