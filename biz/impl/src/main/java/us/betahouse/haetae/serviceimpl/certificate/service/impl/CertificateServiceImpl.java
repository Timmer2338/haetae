/*
 * betahouse.us
 * CopyRight (c) 2012 - 2019
 */
package us.betahouse.haetae.serviceimpl.certificate.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import us.betahouse.haetae.certificate.dal.service.CompetitionRepoService;
import us.betahouse.haetae.certificate.dal.service.QualificationsRepoService;
import us.betahouse.haetae.certificate.dal.service.SkillRepoService;
import us.betahouse.haetae.certificate.enums.CertificateStateEnum;
import us.betahouse.haetae.certificate.enums.CertificateTypeEnum;
import us.betahouse.haetae.certificate.manager.CertificateManager;
import us.betahouse.haetae.certificate.model.basic.CertificateBO;
import us.betahouse.haetae.serviceimpl.certificate.request.CertificateRequest;
import us.betahouse.haetae.serviceimpl.certificate.service.CertificateManagerService;
import us.betahouse.haetae.serviceimpl.certificate.service.CertificateService;
import us.betahouse.haetae.serviceimpl.common.OperateContext;
import us.betahouse.haetae.user.dal.service.UserInfoRepoService;
import us.betahouse.haetae.user.model.basic.UserInfoBO;
import us.betahouse.util.enums.CommonResultCode;
import us.betahouse.util.exceptions.BetahouseException;
import us.betahouse.util.utils.AssertUtil;
import us.betahouse.util.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 证书服务实现
 * <p>
 * 无需鉴权 （普通用户操作）
 *
 * @author guofan.cp
 * @version : CertificateServiceImpl.java 2019/04/06 8:26 guofan.cp
 */
@Service
public class CertificateServiceImpl implements CertificateService {

    @Autowired
    private CertificateManager certificateManager;
    @Autowired
    private CertificateManagerService certificateManagerService;
    @Autowired
    private QualificationsRepoService qualificationsRepoService;
    @Autowired
    private CompetitionRepoService competitionRepoService;
    @Autowired
    private SkillRepoService skillRepoService;
    @Autowired
    private UserInfoRepoService userInfoRepoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CertificateBO create(CertificateRequest request, OperateContext context) {
        //证书类型异常抛出
        CertificateTypeEnum certificateTypeEnum = judgeCertificateType(request);
        CertificateBO certificateBO;
        //证书类型判断(三种)
        switch (certificateTypeEnum) {
            //资格证书
            case QUALIFICATIONS: {
                AssertUtil.assertNotNull(request.getCertificateName(), "发行证书名字不能为空");
                AssertUtil.assertNotNull(request.getCertificateOrganization(), "发行证书组织不能为空");
                AssertUtil.assertNotNull(request.getType(), "资格证书种类不能为空");
                certificateBO = certificateManager.createQualifications(request);
                break;
            }
            //竞赛证书
            case COMPETITION: {
                AssertUtil.assertNotNull(request.getCompetitionName(), "比赛名字不能为空");
                AssertUtil.assertNotNull(request.getRank(), "比赛级别不能为空");
                //重置  stuid转userid
                List<String> userIds = new ArrayList<>();
                for (String stuId : request.getWorkUserId()) {
                    UserInfoBO userInfoBO = userInfoRepoService.queryUserInfoByStuId(stuId);
                    if (userInfoBO == null) {
                        AssertUtil.assertNotNull("队员id不存在");
                    } else {
                        userIds.add(userInfoBO.getUserId());
                    }
                }
                request.setWorkUserId(userIds);
                certificateBO = certificateManager.createCompetition(request);
                AssertUtil.assertNotNull(certificateBO, "BO为空");
                competitionUserIdCovert(certificateBO);
                break;
            }
            //技能证书
            case SKILL: {
                AssertUtil.assertNotNull(request.getCertificateName(), "发行证书名字不能为空");
                AssertUtil.assertNotNull(request.getRank(), "证书等级不能为空");
                certificateBO = certificateManager.createSkill(request);
                break;
            }
            //异常
            default: {
                throw new BetahouseException(CommonResultCode.ILLEGAL_PARAMETERS.getCode(), "证书类型不存在");
            }
        }
        return certificateBO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CertificateBO update(CertificateRequest request, OperateContext context) {
        CertificateTypeEnum certificateTypeEnum = judgeCertificateType(request);
        CertificateBO certificateBO;
        //证书类型判断(三种)
        switch (certificateTypeEnum) {
            //资格证书
            case QUALIFICATIONS: {
                AssertUtil.assertNotNull(request.getCertificateName(), "发行证书名字不能为空");
                AssertUtil.assertNotNull(request.getCertificateOrganization(), "发行证书组织不能为空");
                AssertUtil.assertNotNull(request.getType(), "资格证书种类不能为空");
                certificateBO = certificateManager.modifyQualifications(request);
                break;
            }
            //竞赛证书
            case COMPETITION: {
                AssertUtil.assertNotNull(request.getCompetitionName(), "比赛名字不能为空");
                AssertUtil.assertNotNull(request.getRank(), "比赛级别不能为空");
                certificateBO = certificateManager.modifyCompetition(request);
                competitionUserIdCovert(certificateBO);
                break;
            }
            //技能证书
            case SKILL: {
                AssertUtil.assertNotNull(request.getCertificateName(), "发行证书名字不能为空");
                AssertUtil.assertNotNull(request.getRank(), "证书等级不能为空");
                certificateBO = certificateManager.modifySkill(request);
                break;
            }
            //异常
            default: {
                throw new BetahouseException(CommonResultCode.ILLEGAL_PARAMETERS.getCode(), "证书类型不存在");
            }
        }
        return certificateBO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletebyStudent(CertificateRequest request, OperateContext context) {
        //证书类型异常抛出
        CertificateTypeEnum certificateTypeEnum = judgeCertificateType(request);
        //证书存在异常抛出
        CertificateBO certificateBO = judgeIsExit(request);
        //证书过审异常抛出
        judgeState(certificateBO);
        delete(request, context);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(CertificateRequest request, OperateContext context) {
        //证书类型异常抛出
        CertificateTypeEnum certificateTypeEnum = judgeCertificateType(request);
        //证书存在异常抛出
        CertificateBO certificateBO = judgeIsExit(request);
        switch (certificateTypeEnum) {
            //资格证书
            case QUALIFICATIONS: {
                qualificationsRepoService.deleteByCertificateIdAndUserId(request.getCertificateId(), request.getUserId());
                break;
            }
            //竞赛证书
            case COMPETITION: {
                //判断接受参数获取团队id  和获取证书记录团队id是否相同
                AssertUtil.assertTrue(certificateBO.getTeamId().equals(request.getTeamId()), "竞赛团队id不存在");
                competitionRepoService.deleteAllByTeamId(request.getTeamId());
                break;
            }
            //技能证书
            case SKILL: {
                skillRepoService.deleteByCertificateIdAndUserId(request.getCertificateId(), request.getUserId());
                break;
            }
            //异常
            default: {
                throw new BetahouseException(CommonResultCode.ILLEGAL_PARAMETERS.getCode(), "证书类型不存在");
            }
        }
    }

    @Override
    public CertificateBO findByCertificateId(String certificateId) {
        CertificateBO certificateBO = qualificationsRepoService.queryByCertificateId(certificateId);
        if (certificateBO == null) {
            certificateBO = skillRepoService.queryByCertificateId(certificateId);
        }
        if (certificateBO == null) {
            certificateBO = competitionRepoService.queryByCertificateId(certificateId);
            competitionUserIdCovert(certificateBO);
        }
        AssertUtil.assertNotNull(certificateBO);
        return certificateBO;
    }


    @Override
    public CertificateBO findByCertificateTypeAndId(CertificateRequest request, OperateContext context) {
        //存在性判断返回证书BO
        return judgeIsExit(request);
    }

    @Override
    public List<CertificateBO> findAllByCertificateTypeAndUserId(CertificateRequest request, OperateContext context) {
        //类型异常抛出
        CertificateTypeEnum certificateTypeEnum = judgeCertificateType(request);
        List<CertificateBO> certificateBOS;
        switch (certificateTypeEnum) {
            //资格证书
            case QUALIFICATIONS: {
                certificateBOS = qualificationsRepoService.queryByUserId(request.getUserId());
                break;
            }
            //竞赛证书
            case COMPETITION: {
                certificateBOS = competitionRepoService.queryByUserId(request.getUserId());
                competitionUserIdCovert(certificateBOS);
                break;
            }
            //技能证书
            case SKILL: {
                //同资格证书
                certificateBOS = skillRepoService.queryByUserId(request.getUserId());
                break;
            }
            //异常
            default: {
                throw new BetahouseException(CommonResultCode.ILLEGAL_PARAMETERS.getCode(), "证书类型不存在");
            }
        }
        return certificateBOS;
    }

    /**
     * 判断证书状态抛出异常
     *
     * @param certificateBO
     * @return
     */
    private void judgeState(CertificateBO certificateBO) {
        AssertUtil.assertTrue(certificateBO.getStatus().equals(CertificateStateEnum.UNREVIEWED.getCode()), "证书已通过审核无法删除");
    }

    /**
     * 判断证书类型抛出异常
     *
     * @param request
     * @return
     */
    private CertificateTypeEnum judgeCertificateType(CertificateRequest request) {
        String certificateType = request.getCertificateType();
        CertificateTypeEnum certificateTypeEnum = CertificateTypeEnum.getByCode(certificateType);
        AssertUtil.assertNotNull(certificateTypeEnum, "证书类型不存在");
        return certificateTypeEnum;
    }

    /**
     * 判断证书存在性抛出异常
     * 返回证书实体
     *
     * @param request
     * @return
     */
    private CertificateBO judgeIsExit(CertificateRequest request) {
        //判断证书类型抛出异常
        CertificateTypeEnum certificateTypeEnum = judgeCertificateType(request);
        CertificateBO certificateBO;
        switch (certificateTypeEnum) {
            //资格证书
            case QUALIFICATIONS: {
                //证书存在判断
                certificateBO = qualificationsRepoService.queryByUserIdAndCertificateId(request.getUserId(), request.getCertificateId());
                break;
            }
            //竞赛证书
            case COMPETITION: {
                certificateBO = competitionRepoService.queryByCertificateIdAndUserId(request.getCertificateId(), request.getUserId());
                break;
            }
            //技能证书
            case SKILL: {
                //同资格证书
                certificateBO = skillRepoService.queryByCertificateIdAndUserId(request.getCertificateId(), request.getUserId());
                break;
            }
            //异常
            default: {
                throw new BetahouseException(CommonResultCode.ILLEGAL_PARAMETERS.getCode(), "证书类型不存在");
            }
        }
        //证书非空判断
        AssertUtil.assertNotNull(certificateBO, "证书不存在");
        return certificateBO;
    }

    /**
     * 转换器 竞赛证书userid转 stuid
     *
     * @param certificateBO
     * @return
     */
    private CertificateBO competitionUserIdCovert(CertificateBO certificateBO) {
        List<String> userIds = new ArrayList<>();
        for (String userid : certificateBO.getWorkUserId()) {
            UserInfoBO userInfoBO = userInfoRepoService.queryUserInfoByUserId(userid);
            userIds.add(userInfoBO.getStuId());
        }
        certificateBO.setWorkUserId(userIds);
        return certificateBO;
    }

    /**
     * 转换器 竞赛证书userid 转stuid
     *
     * @param certificateBOS
     * @return
     */
    private List<CertificateBO> competitionUserIdCovert(List<CertificateBO> certificateBOS) {
        return CollectionUtils.toStream(certificateBOS)
                .filter(Objects::nonNull)
                .map(this::competitionUserIdCovert)
                .collect(Collectors.toList());
    }
}