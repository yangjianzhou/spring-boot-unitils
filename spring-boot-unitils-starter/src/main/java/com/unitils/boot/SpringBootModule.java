package com.unitils.boot;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.unitils.core.Module;
import org.unitils.core.TestListener;
import org.unitils.core.Unitils;
import org.unitils.core.UnitilsException;
import org.unitils.database.DatabaseModule;
import org.unitils.database.annotations.Transactional;
import org.unitils.database.transaction.impl.UnitilsTransactionManagementConfiguration;
import org.unitils.spring.annotation.*;
import org.unitils.spring.enums.LoadTime;
import org.unitils.spring.util.ApplicationContextFactory;
import org.unitils.spring.util.ApplicationContextManager;
import org.unitils.util.AnnotationUtils;
import org.unitils.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.unitils.util.AnnotationUtils.*;
import static org.unitils.util.PropertyUtils.getInstance;
import static org.unitils.util.ReflectionUtils.*;

/**
 * @Author: yangjianzhou
 * @Description:
 * @Date:Created in 2018-07-08
 */
public class SpringBootModule implements Module {

    /* Property key of the class name of the application context factory */
    public static final String PROPKEY_APPLICATION_CONTEXT_FACTORY_CLASS_NAME = "SpringModule.applicationContextFactory.implClassName";

    /* Manager for storing and creating org.unitils.spring application contexts */
    private ApplicationContextManager applicationContextManager;

    private  static ApplicationContext applicationContext;

    public static void setApplicationContext(ApplicationContext applicationContext) {
        SpringBootModule.applicationContext = applicationContext;
    }

    /**
     * Initializes this module using the given configuration
     *
     * @param configuration The configuration, not null
     */
    public void init(Properties configuration) {
        // create application context manager that stores and creates the application contexts
        ApplicationContextFactory applicationContextFactory = getInstance(PROPKEY_APPLICATION_CONTEXT_FACTORY_CLASS_NAME, configuration);
        applicationContextManager = new ApplicationContextManager(applicationContextFactory);
    }


    /**
     * No after initialization needed for this module
     */
    public void afterInit() {
        // Make sure that, if a custom transaction manager is configured in the org.unitils.spring ApplicationContext associated with
        // the current test, it is used for managing transactions.
        if (isDatabaseModuleEnabled()) {
            getDatabaseModule().registerTransactionManagementConfiguration(new UnitilsTransactionManagementConfiguration() {

                public boolean isApplicableFor(Object testObject) {
                    if (!isApplicationContextConfiguredFor(testObject)) {
                        return false;
                    }
                    ApplicationContext context = getApplicationContext(testObject);
                    return context.getBeansOfType(getPlatformTransactionManagerClass()).size() != 0;
                }

                @SuppressWarnings("unchecked")
                public PlatformTransactionManager getSpringPlatformTransactionManager(Object testObject) {
                    ApplicationContext context = getApplicationContext(testObject);
                    Class<?> platformTransactionManagerClass = getPlatformTransactionManagerClass();
                    Map<String, PlatformTransactionManager> platformTransactionManagers = (Map<String, PlatformTransactionManager>) context.getBeansOfType(platformTransactionManagerClass);
                    if (platformTransactionManagers.size() == 0) {
                        throw new UnitilsException("Could not find a model of type " + platformTransactionManagerClass.getSimpleName()
                                + " in the org.unitils.spring ApplicationContext for this class");
                    }
                    if (platformTransactionManagers.size() > 1) {
                        Method testMethod = Unitils.getInstance().getTestContext().getTestMethod();
                        String transactionManagerName = getMethodOrClassLevelAnnotationProperty(Transactional.class, "transactionManagerName", "",
                                testMethod, testObject.getClass());
                        if (isEmpty(transactionManagerName))
                            throw new UnitilsException("Found more than one model of type " + platformTransactionManagerClass.getSimpleName()
                                    + " in the org.unitils.spring ApplicationContext for this class. Use the transactionManagerName on the @Transactional"
                                    + " annotation to select the correct one.");
                        if (!platformTransactionManagers.containsKey(transactionManagerName))
                            throw new UnitilsException("No model of type " + platformTransactionManagerClass.getSimpleName()
                                    + " found in the org.unitils.spring ApplicationContext with the name " + transactionManagerName);
                        return platformTransactionManagers.get(transactionManagerName);
                    }
                    return platformTransactionManagers.values().iterator().next();
                }

                public boolean isTransactionalResourceAvailable(Object testObject) {
                    return true;
                }

                public Integer getPreference() {
                    return 20;
                }

                protected Class<?> getPlatformTransactionManagerClass() {
                    return ReflectionUtils.getClassWithName("org.springframework.transaction.PlatformTransactionManager");
                }

            });
        }
    }


    /**
     * Gets the org.unitils.spring model with the given name. The given test instance, by using {@link SpringApplicationContext},
     * determines the application context in which to look for the model.
     * <p/>
     * A UnitilsException is thrown when the no model could be found for the given name.
     *
     * @param testObject The test instance, not null
     * @param name       The name, not null
     * @return The model, not null
     */
    public Object getSpringBean(Object testObject, String name) {
        try {
            return getApplicationContext(testObject).getBean(name);

        } catch (BeansException e) {
            throw new UnitilsException("Unable to get Spring model. No Spring model found for name " + name);
        }
    }

    /**
     * Gets the org.unitils.spring model with the given type. The given test instance, by using {@link SpringApplicationContext},
     * determines the application context in which to look for the model.
     * If more there is not exactly 1 possible model assignment, an UnitilsException will be thrown.
     *
     * @param testObject The test instance, not null
     * @param type       The type, not null
     * @return The model, not null
     */
    public <T> T getSpringBeanByType(Object testObject, Class<T> type) {
        Map<String, T> beans = getApplicationContext(testObject).getBeansOfType(type);
        if (beans == null || beans.size() == 0) {
            throw new UnitilsException("Unable to get Spring model by type. No Spring model found for type " + type.getSimpleName());
        }
        if (beans.size() > 1) {
            throw new UnitilsException("Unable to get Spring model by type. More than one possible Spring model for type " + type.getSimpleName() + ". Possible beans; " + beans);
        }
        return beans.values().iterator().next();
    }

    /**
     * @param testObject The test object
     * @return Whether an ApplicationContext has been configured for the given testObject
     */
    public boolean isApplicationContextConfiguredFor(Object testObject) {
        return applicationContextManager.hasApplicationContext(testObject);
    }


    /**
     * Gets the application context for this test. A new one will be created if it does not exist yet. If a superclass
     * has also declared the creation of an application context, this one will be retrieved (or created if it was not
     * created yet) and used as parent context for this classes context.
     * <p/>
     * If needed, an application context will be created using the settings of the {@link SpringApplicationContext}
     * annotation.
     * <p/>
     * If a class level {@link SpringApplicationContext} annotation is found, the passed locations will be loaded using
     * a <code>ClassPathXmlApplicationContext</code>.
     * Custom creation methods can be created by annotating them with {@link SpringApplicationContext}. They
     * should have an <code>ApplicationContext</code> as return type and either no or exactly 1 argument of type
     * <code>ApplicationContext</code>. In the latter case, the current configured application context is passed as the argument.
     * <p/>
     * A UnitilsException will be thrown if no context could be retrieved or created.
     *
      * @param testObject
     * @return
     */
    public ApplicationContext getApplicationContext(Object testObject) {

        if (applicationContext == null) {
            applicationContext = applicationContextManager.getApplicationContext(testObject);
        }
        return applicationContext;
    }


    /**
     * Forces the reloading of the application context the next time that it is requested. If classes are given
     * only contexts that are linked to those classes will be reset. If no classes are given, all cached
     * contexts will be reset.
     *
     * @param classes The classes for which to reset the contexts
     */
    public void invalidateApplicationContext(Class<?>... classes) {
        applicationContextManager.invalidateApplicationContext(classes);
        applicationContext = null;
    }


    /**
     * Gets the application context for this class and sets it on the fields and setter methods that are
     * annotated with {@link SpringApplicationContext}. If no application context could be created, an
     * UnitilsException will be raised.
     *
     * @param testObject The test instance, not null
     */
    public void injectApplicationContext(Object testObject) {


        // inject into fields annotated with @SpringApplicationContext
        Set<Field> fields = getFieldsAnnotatedWith(testObject.getClass(), SpringApplicationContext.class);
        for (Field field : fields) {
            try {
                setFieldValue(testObject, field, getApplicationContext(testObject));
            } catch (UnitilsException e) {
                throw new UnitilsException("Unable to assign the application context to field annotated with @" + SpringApplicationContext.class.getSimpleName(), e);
            }
        }

        // inject into setter methods annotated with @SpringApplicationContext
        Set<Method> methods = getMethodsAnnotatedWith(testObject.getClass(), SpringApplicationContext.class, false);
        for (Method method : methods) {
            // ignore custom create methods
            if (method.getReturnType() != Void.TYPE) {
                continue;
            }
            try {
                invokeMethod(testObject, method, getApplicationContext(testObject));

            } catch (Exception e) {
                throw new UnitilsException("Unable to assign the application context to setter annotated with @" + SpringApplicationContext.class.getSimpleName(), e);
            }
        }
    }


    /**
     * Injects org.unitils.spring beans into all fields that are annotated with {@link SpringBean}.
     *
     * @param testObject The test instance, not null
     */
    public void injectSpringBeans(Object testObject) {
        // assign to fields
        Set<Field> fields = getFieldsAnnotatedWith(testObject.getClass(), SpringBean.class);
        for (Field field : fields) {
            try {
                SpringBean springBeanAnnotation = field.getAnnotation(SpringBean.class);
                setFieldValue(testObject, field, getSpringBean(testObject, springBeanAnnotation.value()));

            } catch (UnitilsException e) {
                throw new UnitilsException("Unable to assign the Spring model value to field annotated with @" + SpringBean.class.getSimpleName(), e);
            }
        }

        // assign to setters
        Set<Method> methods = getMethodsAnnotatedWith(testObject.getClass(), SpringBean.class);
        for (Method method : methods) {
            try {
                if (!isSetter(method)) {
                    throw new UnitilsException("Unable to assign the Spring model value to method annotated with @" + SpringBean.class.getSimpleName() + ". Method " +
                            method.getName() + " is not a setter method.");
                }
                SpringBean springBeanAnnotation = method.getAnnotation(SpringBean.class);
                invokeMethod(testObject, method, getSpringBean(testObject, springBeanAnnotation.value()));

            } catch (UnitilsException e) {
                throw new UnitilsException("Unable to assign the Spring model value to method annotated with @" + SpringBean.class.getSimpleName(), e);
            } catch (InvocationTargetException e) {
                throw new UnitilsException("Unable to assign the Spring model value to method annotated with @" + SpringBean.class.getSimpleName() + ". Method " +
                        "has thrown an exception.", e.getCause());
            }
        }
    }


    /**
     * Injects org.unitils.spring beans into all fields methods that are annotated with {@link SpringBeanByType}.
     *
     * @param testObject The test instance, not null
     */
    public void injectSpringBeansByType(Object testObject) {
        // assign to fields
        Set<Field> fields = getFieldsAnnotatedWith(testObject.getClass(), SpringBeanByType.class);
        for (Field field : fields) {
            try {
                setFieldValue(testObject, field, getSpringBeanByType(testObject, field.getType()));

            } catch (UnitilsException e) {
                throw new UnitilsException("Unable to assign the Spring model value to field annotated with @" + SpringBeanByType.class.getSimpleName(), e);
            }
        }

        // assign to setters
        Set<Method> methods = getMethodsAnnotatedWith(testObject.getClass(), SpringBeanByType.class);
        for (Method method : methods) {
            try {
                if (!isSetter(method)) {
                    throw new UnitilsException("Unable to assign the Spring model value to method annotated with @" + SpringBeanByType.class.getSimpleName() + ". Method " +
                            method.getName() + " is not a setter method.");
                }
                invokeMethod(testObject, method, getSpringBeanByType(testObject, method.getParameterTypes()[0]));

            } catch (UnitilsException e) {
                throw new UnitilsException("Unable to assign the Spring model value to method annotated with @" + SpringBeanByType.class.getSimpleName(), e);
            } catch (InvocationTargetException e) {
                throw new UnitilsException("Unable to assign the Spring model value to method annotated with @" + SpringBeanByType.class.getSimpleName() + ". Method " +
                        "has thrown an exception.", e.getCause());
            }
        }
    }


    /**
     * Injects org.unitils.spring beans into all fields that are annotated with {@link SpringBeanByName}.
     *
     * @param testObject The test instance, not null
     */
    public void injectSpringBeansByName(Object testObject) {
        // assign to fields
        Set<Field> fields = getFieldsAnnotatedWith(testObject.getClass(), SpringBeanByName.class);
        for (Field field : fields) {
            try {
                setFieldValue(testObject, field, getSpringBean(testObject, field.getName()));

            } catch (UnitilsException e) {
                throw new UnitilsException("Unable to assign the Spring model value to field annotated with @" + SpringBeanByName.class.getSimpleName(), e);
            }
        }

        // assign to setters
        Set<Method> methods = getMethodsAnnotatedWith(testObject.getClass(), SpringBeanByName.class);
        for (Method method : methods) {
            try {
                if (!isSetter(method)) {
                    throw new UnitilsException("Unable to assign the Spring model value to method annotated with @" + SpringBeanByName.class.getSimpleName() + ". Method " +
                            method.getName() + " is not a setter method.");
                }
                invokeMethod(testObject, method, getSpringBean(testObject, getPropertyName(method)));

            } catch (UnitilsException e) {
                throw new UnitilsException("Unable to assign the Spring model value to method annotated with @" + SpringBeanByName.class.getSimpleName(), e);
            } catch (InvocationTargetException e) {
                throw new UnitilsException("Unable to assign the Spring model value to method annotated with @" + SpringBeanByName.class.getSimpleName() + ". Method " +
                        "has thrown an exception.", e.getCause());
            }
        }
    }

    protected void closeApplicationContextIfNeeded(Object testObject) {
        if (this.isApplicationContextConfiguredFor(testObject)) {
            this.invalidateApplicationContext(testObject.getClass());
        }
    }

    protected boolean isDatabaseModuleEnabled() {
        return Unitils.getInstance().getModulesRepository().isModuleEnabled(DatabaseModule.class);
    }


    protected DatabaseModule getDatabaseModule() {
        return Unitils.getInstance().getModulesRepository().getModuleOfType(DatabaseModule.class);
    }

    public void initialize(Object testObject) {
        injectApplicationContext(testObject);
        injectSpringBeans(testObject);
        injectSpringBeansByType(testObject);
        injectSpringBeansByName(testObject);
    }


    /**
     * @return The {@link TestListener} for this module
     */
    public TestListener getTestListener() {
        return new SpringTestListener();
    }

    public LoadTime findLoadTime(Class<?> clzz) {
        LoadOn loadOnAnnotation = AnnotationUtils.getClassLevelAnnotation(LoadOn.class, clzz);
        if (loadOnAnnotation == null) {
            return LoadTime.METHOD;
        } else {
            return loadOnAnnotation.load();
        }

    }

    /**
     * The {@link TestListener} for this module
     */
    protected class SpringTestListener extends TestListener {

        @Override
        public void beforeTestSetUp(Object testObject, Method testMethod) {
            if (findLoadTime(testObject.getClass()) == LoadTime.METHOD || applicationContext == null) {
                initialize(testObject);
            }
        }

        /**
         * @see TestListener#afterTestTearDown(Object, Method)
         */
        @Override
        public void afterTestTearDown(Object testObject, Method testMethod) {
            if (findLoadTime(testObject.getClass()) == LoadTime.METHOD) {
                closeApplicationContextIfNeeded(testObject);
            }

        }

    }

}
