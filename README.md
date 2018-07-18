# spring-boot-unitils
第一天：  
利用unnitils.properties来添加配置，并且由用户来维护

第二天：  
完全的spring boot配置，将一些固定的配置放在unitils.properties里面，并将其  
打包到了spring-boot-unitils-starter的jar中，单元测试运行时自动去加载unitils.properties  
这个文件,在unitils.properties指定了unitils.configuration.localFileName的值为  
application-ut.properties，因此用户单元应用目录下需要有这个文件，这里面配置一些用户应用独特  
的配置，例如数据库之类的

第三天
上传了1.1.0.RELEASE，修复了1.0.0.RELEASE中的datasource没使用unitils的bug，以及将spring相关的依赖限制为provided