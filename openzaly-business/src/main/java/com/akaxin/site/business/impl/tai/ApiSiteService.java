/**
 * Copyright 2018-2028 Akaxin Group
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.akaxin.site.business.impl.tai;

import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.akaxin.common.command.Command;
import com.akaxin.common.command.CommandResponse;
import com.akaxin.common.constant.CommandConst;
import com.akaxin.common.constant.ErrorCode2;
import com.akaxin.common.constant.IErrorCode;
import com.akaxin.common.crypto.HashCrypto;
import com.akaxin.common.crypto.RSACrypto;
import com.akaxin.common.exceptions.ZalyException2;
import com.akaxin.common.logs.LogUtils;
import com.akaxin.common.utils.StringHelper;
import com.akaxin.common.utils.UserIdUtils;
import com.akaxin.proto.core.ConfigProto;
import com.akaxin.proto.core.CoreProto;
import com.akaxin.proto.core.UserProto;
import com.akaxin.proto.site.ApiSiteConfigProto;
import com.akaxin.proto.site.ApiSiteLoginProto;
import com.akaxin.proto.site.ApiSiteRegisterProto;
import com.akaxin.site.business.bean.PlatformPhoneBean;
import com.akaxin.site.business.dao.SiteConfigDao;
import com.akaxin.site.business.dao.SiteLoginDao;
import com.akaxin.site.business.dao.UserFriendDao;
import com.akaxin.site.business.dao.UserGroupDao;
import com.akaxin.site.business.dao.UserProfileDao;
import com.akaxin.site.business.impl.AbstractRequest;
import com.akaxin.site.business.impl.notice.User2Notice;
import com.akaxin.site.business.impl.site.PlatformUserPhone;
import com.akaxin.site.business.impl.site.SiteConfig;
import com.akaxin.site.business.impl.site.UserUic;
import com.akaxin.site.storage.api.IUserDeviceDao;
import com.akaxin.site.storage.bean.ApplyFriendBean;
import com.akaxin.site.storage.bean.SimpleUserBean;
import com.akaxin.site.storage.bean.UserDeviceBean;
import com.akaxin.site.storage.bean.UserProfileBean;
import com.akaxin.site.storage.bean.UserSessionBean;
import com.akaxin.site.storage.service.DeviceDaoService;

/**
 * ?????????????????? <br>
 * ????????????&&??????
 *
 * @author Sam{@link an.guoyue254@gmail.com}
 * @since 2017-10-17 18:14:10
 */
public class ApiSiteService extends AbstractRequest {

	private static final Logger logger = LoggerFactory.getLogger(ApiSiteService.class);
	private static final int DEFAULT_PORT = 2021;
	private IUserDeviceDao userDeviceDao = new DeviceDaoService();

	/**
	 * <pre>
	 * 		api.site.config
	 * 		????????????????????????
	 * </pre>
	 *
	 * @param command
	 * @return
	 */
	public CommandResponse config(Command command) {
		CommandResponse commandResponse = new CommandResponse().setAction(CommandConst.ACTION_RES);
		ErrorCode2 errCode = ErrorCode2.ERROR;
		try {
			Map<Integer, String> configMap = SiteConfig.getConfigMap();
			LogUtils.requestDebugLog(logger, command, "");

			if (configMap != null) {
				ConfigProto.SiteConfig.Builder configBuilder = ConfigProto.SiteConfig.newBuilder();
				if (StringUtils.isNotBlank(configMap.get(ConfigProto.ConfigKey.SITE_ADDRESS_VALUE))) {
					configBuilder.setSiteAddress(configMap.get(ConfigProto.ConfigKey.SITE_ADDRESS_VALUE));
				}
				if (StringUtils.isNumeric(configMap.get(ConfigProto.ConfigKey.SITE_PORT_VALUE))) {
					configBuilder.setSitePort(Integer.valueOf(configMap.get(ConfigProto.ConfigKey.SITE_PORT_VALUE)));
				}
				if (StringUtils.isNotBlank(configMap.get(ConfigProto.ConfigKey.SITE_NAME_VALUE))) {
					configBuilder.setSiteName(configMap.get(ConfigProto.ConfigKey.SITE_NAME_VALUE));
				}
				if (StringUtils.isNotBlank(configMap.get(ConfigProto.ConfigKey.SITE_LOGO_VALUE))) {
					configBuilder.setSiteLogo(configMap.get(ConfigProto.ConfigKey.SITE_LOGO_VALUE));
				}
				if (StringUtils.isNotBlank(configMap.get(ConfigProto.ConfigKey.SITE_VERSION_VALUE))) {
					configBuilder.setSiteVersion(configMap.get(ConfigProto.ConfigKey.SITE_VERSION_VALUE));
				}
				if (StringUtils.isNotBlank(configMap.get(ConfigProto.ConfigKey.SITE_INTRODUCTION_VALUE))) {
					configBuilder.setSiteIntroduction(configMap.get(ConfigProto.ConfigKey.SITE_INTRODUCTION_VALUE));
				}
				if (StringUtils.isNotBlank(configMap.get(ConfigProto.ConfigKey.PIC_SIZE_VALUE))) {
					configBuilder.setPicSize(configMap.get(ConfigProto.ConfigKey.PIC_SIZE_VALUE));
				}
				if (StringUtils.isNumeric(configMap.get(ConfigProto.ConfigKey.REALNAME_STATUS_VALUE))) {
					configBuilder.setRealNameConfigValue(
							Integer.valueOf(configMap.get(ConfigProto.ConfigKey.REALNAME_STATUS_VALUE)));
				}
				if (StringUtils.isNumeric(configMap.get(ConfigProto.ConfigKey.INVITE_CODE_STATUS_VALUE))) {
					configBuilder.setInviteCodeConfigValue(
							Integer.valueOf(configMap.get(ConfigProto.ConfigKey.INVITE_CODE_STATUS_VALUE)));
				}
				ApiSiteConfigProto.ApiSiteConfigResponse response = ApiSiteConfigProto.ApiSiteConfigResponse
						.newBuilder().setSiteConfig(configBuilder.build()).build();
				commandResponse.setParams(response.toByteArray());
				errCode = ErrorCode2.SUCCESS;
			}

		} catch (Exception e) {
			errCode = ErrorCode2.ERROR_SYSTEMERROR;
			LogUtils.requestErrorLog(logger, command, e);
		}
		return commandResponse.setErrCode2(errCode);
	}

	public CommandResponse register(Command command) {
		CommandResponse commandResponse = new CommandResponse().setAction(CommandConst.ACTION_RES);
		IErrorCode errCode = ErrorCode2.ERROR;
		try {
			ApiSiteRegisterProto.ApiSiteRegisterRequest request = ApiSiteRegisterProto.ApiSiteRegisterRequest
					.parseFrom(command.getParams());
			String userIdPubk = request.getUserIdPubk();
			String userName = request.getUserName();
			String userPhoto = request.getUserPhoto();
			String userUic = request.getUserUic();
			String applyInfo = request.getApplyInfo();
			String phoneToken = request.getPhoneToken();
			String fullPhoneId = null;// ??????phoneToken ????????? counrtyCode:phoneId
			String siteUserId = UUID.randomUUID().toString();// siteUserId??????????????????
			String siteLoginId = request.getSiteLoginId();// ????????????

			LogUtils.requestDebugLog(logger, command, request.toString());

			if (StringUtils.isAnyEmpty(userIdPubk, userName)) {
				throw new ZalyException2(ErrorCode2.ERROR_PARAMETER);
			}

			if (userName.length() > 16) {
				throw new ZalyException2(ErrorCode2.ERROR_PARAMETER_NICKNAME);
			}

			// ??????????????????
			ConfigProto.RealNameConfig realNameConfig = SiteConfig.getRealNameConfig();
			switch (realNameConfig) {
			case REALNAME_YES:
				if (StringUtils.isNotBlank(phoneToken)) {
					PlatformPhoneBean bean = PlatformUserPhone.getInstance().getPhoneIdFromPlatform(phoneToken);
					fullPhoneId = bean.getFullPhoneId();
					logger.debug("user realname register???get phoneid from platform???{}", fullPhoneId);

					if (StringUtils.isEmpty(fullPhoneId)) {
						throw new ZalyException2(ErrorCode2.ERROR_REGISTER_PHONEID);
					}
				} else {
					throw new ZalyException2(ErrorCode2.ERROR_REGISTER_PHONETOKEN);
				}
				break;
			default:
				break;
			}

			// ?????????????????????
			ConfigProto.InviteCodeConfig uicConfig = SiteConfig.getUICConfig();
			switch (uicConfig) {
			case UIC_YES:
				logger.debug("??????????????????????????????");
				if (!UserUic.getInstance().checkUic(userUic, siteUserId)) {
					throw new ZalyException2(ErrorCode2.ERROR_REGISTER_UIC);
				}
				break;
			default:
				break;
			}

			UserProfileBean regBean = new UserProfileBean();
			regBean.setSiteUserId(siteUserId);
			regBean.setUserIdPubk(userIdPubk);
			regBean.setUserName(userName);
			regBean.setUserNameInLatin(StringHelper.toLatinPinYin(userName));
			regBean.setApplyInfo(applyInfo);
			regBean.setUserPhoto(userPhoto);
			regBean.setPhoneId(fullPhoneId);
			regBean.setUserStatus(UserProto.UserStatus.NORMAL_VALUE);
			regBean.setRegisterTime(System.currentTimeMillis());

			if (SiteLoginDao.getInstance().registerUser(regBean)) {
				ApiSiteRegisterProto.ApiSiteRegisterResponse response = ApiSiteRegisterProto.ApiSiteRegisterResponse
						.newBuilder().setSiteUserId(siteUserId).build();
				commandResponse.setParams(response.toByteArray());
				errCode = ErrorCode2.SUCCESS;
			} else {
				errCode = ErrorCode2.ERROR_REGISTER_USERID_UNIQUE;
			}

			if (ErrorCode2.SUCCESS == errCode) {
				addUserDefaultFriendsAndGroups(siteUserId);
				// ???????????????????????????????????????????????????
				justForAdminUser(siteUserId, command.getHeader());
			}
		} catch (Exception e) {
			errCode = ErrorCode2.ERROR_SYSTEMERROR;
			LogUtils.requestErrorLog(logger, command, e);
		} catch (ZalyException2 e) {
			errCode = e.getErrCode();
			LogUtils.requestErrorLog(logger, command, e);
		}
		return commandResponse.setErrCode(errCode);
	}

	// ??????????????????????????????
	private void addUserDefaultFriendsAndGroups(String siteUserId) {
		try {
			Set<String> defaultFriends = SiteConfig.getUserDefaultFriends();
			if (defaultFriends != null && defaultFriends.size() > 0) {
				for (String siteFriendId : defaultFriends) {
					UserFriendDao.getInstance().saveFriendApply(siteFriendId, siteUserId, "???????????????????????????????????????????????????");
					UserFriendDao.getInstance().agreeApply(siteUserId, siteFriendId, true);
					ApplyFriendBean applyBean = UserFriendDao.getInstance().agreeApplyWithClear(siteUserId,
							siteFriendId, true);
					new User2Notice().addFriendTextMessage(applyBean);
				}
			}
			logger.debug("{} add default friends={}", siteUserId, defaultFriends);
		} catch (Exception e) {
			logger.error("add default friends error", e);
		}

		try {
			Set<String> defaultGroups = SiteConfig.getUserDefaultGroups();
			if (defaultGroups != null && defaultGroups.size() > 0) {
				for (String groupId : defaultGroups) {
					UserGroupDao.getInstance().addDefaultGroupMember(groupId, siteUserId);
				}
			}
			logger.debug("{} add default groups={}", siteUserId, defaultGroups);
		} catch (Exception e) {
			logger.error("add user default groups error", e);
		}

	}

	private void justForAdminUser(String siteUserId, Map<Integer, String> header) {
		try {
			// ???????????????????????????
			if (SiteConfig.hasNoAdminUser()) {
				logger.debug("user first time to register site server ,set it as admin:{} map:{}", siteUserId, header);
				SiteConfigDao.getInstance().updateSiteConfig(ConfigProto.ConfigKey.SITE_ADMIN_VALUE, siteUserId);
				if (header != null) {
					String host = header.get(CoreProto.HeaderKey.CLIENT_REQUEST_SERVER_HOST_VALUE);
					if (StringUtils.isNotEmpty(host)) {
						SiteConfigDao.getInstance().updateSiteConfig(ConfigProto.ConfigKey.SITE_ADDRESS_VALUE, host);
						SiteConfigDao.getInstance().updateSiteConfig(ConfigProto.ConfigKey.SITE_NAME_VALUE, host);
					}
					String port = header.get(CoreProto.HeaderKey.CLIENT_REQUEST_SERVER_HOST_VALUE);
					if (StringUtils.isNotBlank(port)) {
						port = "" + DEFAULT_PORT;
						SiteConfigDao.getInstance().updateSiteConfig(ConfigProto.ConfigKey.SITE_PORT_VALUE, port);
					}
					// ???????????????????????????
					SiteConfigDao.getInstance().updateSiteConfig(ConfigProto.ConfigKey.INVITE_CODE_STATUS_VALUE,
							ConfigProto.InviteCodeConfig.UIC_NO_VALUE + "");
				}
				SiteConfig.updateConfig();
			}
		} catch (Exception e) {
			logger.error("set site admin error", e);
		}
	}

	/**
	 * ??????????????????????????????
	 *
	 * @param command
	 * @return
	 */
	public CommandResponse login(Command command) {
		CommandResponse commandResponse = new CommandResponse().setAction(CommandConst.ACTION_RES);
		IErrorCode errCode = ErrorCode2.ERROR;
		try {
			ApiSiteLoginProto.ApiSiteLoginRequest loginRequest = ApiSiteLoginProto.ApiSiteLoginRequest
					.parseFrom(command.getParams());
			String userIdPubk = loginRequest.getUserIdPubk();
			String userIdSignBase64 = loginRequest.getUserIdSignBase64();
			String userDeviceIdPubk = loginRequest.getUserDeviceIdPubk();
			String userDeviceIdSignBase64 = loginRequest.getUserDeviceIdSignBase64();
			String userDeviceName = loginRequest.getUserDeviceName();
			String userToken = loginRequest.getUserToken();
			String phoneToken = loginRequest.getPhoneToken();
			LogUtils.requestDebugLog(logger, command, loginRequest.toString());

			if (StringUtils.isAnyEmpty(userIdPubk, userIdSignBase64)) {
				throw new ZalyException2(ErrorCode2.ERROR2_LOGGIN_USERID_EMPTY);
			}

			if (StringUtils.isAnyEmpty(userDeviceIdPubk, userDeviceIdSignBase64)) {
				throw new ZalyException2(ErrorCode2.ERROR2_LOGGIN_DEVICEID_EMPTY);
			}

			// if (StringUtils.isEmpty(userToken)) {
			// throw new ZalyException2(ErrorCode2.ERROR2_LOGGIN_USERTOKEN_EMPTY);
			// }

			PublicKey userPubKey = RSACrypto.getRSAPubKeyFromPem(userIdPubk);// ???????????????????????????Sign???????????????Key
			Signature userSign = Signature.getInstance("SHA512withRSA");
			userSign.initVerify(userPubKey);
			userSign.update(userIdPubk.getBytes());// ??????
			boolean userSignResult = userSign.verify(Base64.getDecoder().decode(userIdSignBase64));
			logger.debug("userSignResult={}", userSignResult);

			if (userSignResult) {
				Signature userDeviceSign = Signature.getInstance("SHA512withRSA");
				userDeviceSign.initVerify(userPubKey);
				userDeviceSign.update(userDeviceIdPubk.getBytes());// ??????
				userSignResult = userDeviceSign.verify(Base64.getDecoder().decode(userDeviceIdSignBase64));
			}
			logger.debug("deviceSignResult={}", userSignResult);

			// ???????????????????????????????????????????????????
			if (!userSignResult) {
				throw new ZalyException2(ErrorCode2.ERROR2_LOGGIN_ERRORSIGN);
			}

			// ?????????????????????????????????
			String globalUserId = verifyPlatformPhoneAndGetGlobalUserId(userIdPubk, phoneToken);

			// ?????????????????????????????????,??????????????????
			SimpleUserBean subean = UserProfileDao.getInstance().getSimpleProfileByGlobalUserId(globalUserId, true);
			if (subean == null || StringUtils.isEmpty(subean.getUserId())) {
				logger.info("login site: new user need to register before login site");
				errCode = ErrorCode2.ERROR2_LOGGIN_NOREGISTER;// ?????????,??????????????????????????????
				return commandResponse.setErrCode(errCode);
			}

			if (subean.getUserStatus() == UserProto.UserStatus.SEALUP_VALUE) {
				logger.info("login site:	 user no permision as sealed up");
				errCode = ErrorCode2.ERROR2_LOGGIN_SEALUPUSER;// ????????????????????????
				return commandResponse.setErrCode(errCode);
			}

			String siteUserId = subean.getUserId();
			String deviceId = HashCrypto.MD5(userDeviceIdPubk);

			// ??????????????????
			UserDeviceBean deviceBean = new UserDeviceBean();
			deviceBean.setDeviceId(deviceId);
			deviceBean.setDeviceName(userDeviceName);
			deviceBean.setSiteUserId(siteUserId);
			deviceBean.setUserDevicePubk(userDeviceIdPubk);
			deviceBean.setUserToken(userToken);
			deviceBean.setActiveTime(System.currentTimeMillis());
			deviceBean.setAddTime(System.currentTimeMillis());

			boolean loginResult = SiteLoginDao.getInstance().updateUserDevice(deviceBean);

			if (!loginResult) {// ????????????????????????????????????
				loginResult = SiteLoginDao.getInstance().saveUserDevice(deviceBean);
				// ?????????????????????????????????????????????
				limitUserDevice(siteUserId);
			}

			logger.debug("login site: save device result={} deviceBean={}", loginResult, deviceBean.toString());

			if (!loginResult) {
				// ??????????????????
				throw new ZalyException2(ErrorCode2.ERROR2_LOGGIN_UPDATE_DEVICE);
			}

			// ??????session
			String sessionId = UUID.randomUUID().toString();

			UserSessionBean sessionBean = new UserSessionBean();
			sessionBean.setLoginTime(System.currentTimeMillis());
			sessionBean.setSiteUserId(siteUserId);
			sessionBean.setOnline(true);
			sessionBean.setSessionId(sessionId);
			sessionBean.setDeviceId(deviceId);
			sessionBean.setLoginTime(System.currentTimeMillis());// ????????????(auth)??????

			loginResult = loginResult && SiteLoginDao.getInstance().saveUserSession(sessionBean);

			if (loginResult) {
				ApiSiteLoginProto.ApiSiteLoginResponse response = ApiSiteLoginProto.ApiSiteLoginResponse.newBuilder()
						.setSiteUserId(siteUserId).setUserSessionId(sessionId).build();
				commandResponse.setParams(response.toByteArray());
				errCode = ErrorCode2.SUCCESS;
			} else {
				errCode = ErrorCode2.ERROR2_LOGGIN_UPDATE_SESSION;
			}
		} catch (ZalyException2 e) {
			errCode = e.getErrCode();
			LogUtils.requestErrorLog(logger, command, e);
		} catch (Exception e) {
			errCode = ErrorCode2.ERROR_SYSTEMERROR;
			LogUtils.requestErrorLog(logger, command, e);
		}
		return commandResponse.setErrCode(errCode);
	}

	private String verifyPlatformPhoneAndGetGlobalUserId(String userIdPubk, String phoneToken) throws ZalyException2 {
		phoneToken = null;
		if (StringUtils.isEmpty(phoneToken)) {
			logger.debug("api.site.login with phoneToken={}", phoneToken);
			return UserIdUtils.getV1GlobalUserId(userIdPubk);
		}

		// ?????????????????????????????????
		ConfigProto.RealNameConfig realNameConfig = SiteConfig.getRealNameConfig();

		if (ConfigProto.RealNameConfig.REALNAME_YES == realNameConfig) {
			PlatformPhoneBean bean = PlatformUserPhone.getInstance().getPhoneIdFromPlatform(phoneToken);
			String fullPhoneId = bean.getFullPhoneId();
			String platformUserIdPubk = bean.getUserIdPubk();
			logger.debug("get platform realname phone info bean={}", bean);

			if (StringUtils.isEmpty(fullPhoneId)) {
				return UserIdUtils.getV1GlobalUserId(userIdPubk);
			}

			if (!userIdPubk.equals(platformUserIdPubk)) {
				logger.error("api.site.login equals={} userIdPubk={} platformUserIdPubk={}", false, userIdPubk,
						platformUserIdPubk);
				return UserIdUtils.getV1GlobalUserId(userIdPubk);
			}

			// ???????????????????????????
			UserProfileBean profile = UserProfileDao.getInstance().getUserProfileByFullPhoneId(fullPhoneId);

			if (profile != null && StringUtils.isNoneEmpty(platformUserIdPubk, profile.getUserIdPubk())) {

				if (platformUserIdPubk.equals(profile.getUserIdPubk())) {
					logger.debug("new site realname login verifyPlatformPhone success");
					return UserIdUtils.getV1GlobalUserId(platformUserIdPubk);
				} else {
					// ????????????
					String globalUserId = UserIdUtils.getV1GlobalUserId(platformUserIdPubk);
					boolean updateRes = UserProfileDao.getInstance().updateUserIdPubk(profile.getSiteUserId(),
							globalUserId, userIdPubk);

					if (!updateRes) {
						throw new ZalyException2(ErrorCode2.ERROR2_LOGGIN_UPDATENEWPUBK);
					}
				}

			}

		}

		return UserIdUtils.getV1GlobalUserId(userIdPubk);
	}

	// ???????????????????????????
	private void limitUserDevice(String siteUserId) {
		try {
			int limitNumber = 4;
			userDeviceDao.limitDeviceNum(siteUserId, limitNumber);
		} catch (Exception e) {
			logger.error(StringHelper.format("limit siteUserId={} device num error", siteUserId), e);
		}
	}
}
