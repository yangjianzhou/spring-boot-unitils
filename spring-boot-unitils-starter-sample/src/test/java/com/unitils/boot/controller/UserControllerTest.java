package com.unitils.boot.controller;

import com.unitils.boot.SampleTestApplication;
import com.unitils.boot.util.UnitilsBootBlockJUnit4ClassRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.unitils.database.annotations.Transactional;
import org.unitils.database.util.TransactionMode;
import org.unitils.dbunit.annotation.DataSet;
import org.unitils.spring.annotation.SpringBeanByType;

@RunWith(UnitilsBootBlockJUnit4ClassRunner.class)
@SpringBootTest(classes = SampleTestApplication.class)
@Transactional(value = TransactionMode.ROLLBACK)
public class UserControllerTest {

    @SpringBeanByType
    private UserController userController ;

    @Test
    @DataSet(value = {"/data/getUserInfo.xls"})
    public void test_getUsername(){
        String username = userController.getUsername(3);
        Assert.assertNotNull(username);
        Assert.assertTrue(username.equals("wangwu"));
    }

    @Test
    @DataSet(value = {"/data/getUserInfo.xls"})
    public void test_getUsername1(){
        String username = userController.getUsername(3);
        Assert.assertNotNull(username);
        Assert.assertTrue(username.equals("wangwu"));
    }
}
