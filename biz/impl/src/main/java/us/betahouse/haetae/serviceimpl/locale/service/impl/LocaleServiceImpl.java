/*
  betahouse.us
  CopyRight (c) 2012 - 2019
 */
package us.betahouse.haetae.serviceimpl.locale.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import us.betahouse.haetae.locale.manager.LocaleManager;
import us.betahouse.haetae.locale.model.basic.LocaleBO;
import us.betahouse.haetae.serviceimpl.common.OperateContext;
import us.betahouse.haetae.serviceimpl.locale.request.LocaleManagerRequest;
import us.betahouse.haetae.serviceimpl.locale.service.LocaleService;

import java.util.List;

/**
 * @author NathanDai
 * @version :  2019-07-03 21:24 NathanDai
 */

@Service
public class LocaleServiceImpl implements LocaleService {
    @Autowired
    LocaleManager localeManager;

    /**
     * 查询所有可以用的场地
     *
     * @param request
     * @param context
     * @return
     */
    @Override
    public List<LocaleBO> findAllLocale(LocaleManagerRequest request, OperateContext context) {
        List<LocaleBO> localeBOList = localeManager.findAll();
        return localeBOList;
    }

    @Override
    public List<LocaleBO> findLocaleByStatus(LocaleManagerRequest request, OperateContext context) {
        List<LocaleBO> localeBOList = localeManager.findByStatus(request.getStatus());
        return localeBOList;
    }
}
