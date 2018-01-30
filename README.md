
# OAuth 2.0 简介

`OAuth 2.0`是一种工业级的授权协议。OAuth 2.0是从创建于2006年的OAuth 1.0继承而来的。OAuth 2.0致力于帮助开发者简化授权并为web应用、桌面应用、移动应用、嵌入式应用提供具体的授权流程。

> OAuth 2.0 is the industry-standard protocol for authorization. OAuth 2.0 supersedes the work done on the original OAuth protocol created in 2006. OAuth 2.0 focuses on client developer simplicity while providing specific authorization flows for web applications, desktop applications, mobile phones, and living room devices. 

## OAuth 2.0的四个角色

为了方便理解，以常用的`使用微信登录`为例

* **Resource Owner**

    资源拥有者，对应微信的每个用户微信上设置的个人信息是属于每个用户的，不属于腾讯。

* **Resource Server**

    资源服务器，一般就是用户数据的一些操作（增删改查）的REST API，比如微信的获取用户基本信息的接口。
    
* **Client Application**

    第三方客户端，对比微信中就是各种微信公众号开发的应用，第三方应用经过`认证服务器`授权后即可访问`资源服务器`的REST API来获取用户的头像、性别、地区等基本信息。

* **Authorization Server**

    认证服务器，验证第三方客户端是否合法。如果合法就给客户端颁布token，第三方通过token来调用资源服务器的API。

## 四种授权方式（Grant Type）

* **anthorization_code** 

    授权码类型，适用于Web Server Application。模式为：客户端先调用`/oauth/authorize/`进到用户授权界面，用户授权后返回`code`，客户端然后根据code和`appSecret`获取`access token`。

* **implicit**
    简化类型，相对于授权码类型少了授权码获取的步骤。客户端应用授权后认证服务器会直接将access token放在客户端的url。客户端解析url获取token。这种方式其实是不太安全的，可以通过**https安全通道**和**缩短access token的有效时间**来较少风险。

* **password**

    密码类型，客户端应用通过用户的username和password获access token。适用于资源服务器、认证服务器与客户端具有完全的信任关系，因为要将用户要将用户的用户名密码直接发送给客户端应用，客户端应用通过用户发送过来的用户名密码获取token，然后访问资源服务器资源。比如支付宝就可以直接用淘宝用户名和密码登录，因为它们属于同一家公司，彼此**充分信任**。

* **client_credentials**

    客户端类型，是不需要用户参与的一种方式，用于不同服务之间的对接。比如自己开发的应用程序要调用短信验证码服务商的服务，调用地图服务商的服务、调用手机消息推送服务商的服务。当需要调用服务是可以直接使用服务商给的`appID`和`appSecret`来获取token，得到token之后就可以直接调用服务。
    
## 其他概念

* **scope**：访问资源服务器的哪些作用域。
* **refresh token**：当access token 过期后，可以通过refresh token重新获取access token。

# 实现

有的时候资源服务器和认证服务器是两个不同的应用，有的时候资源服务器和认证服务器在通一个应用中，不同之处在于资源服务器是否需要检查token的有效性，前者需要检查，后者不需要。这里实现后者。

## Application的安全配置

``` java
@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.formLogin()
                .and().csrf().disable()
                .authorizeRequests().anyRequest().authenticated();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        super.configure(web);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().withUser("lyt").password("lyt").authorities("ROLE_USER")
                .and().withUser("admin").password("admin").authorities("ROLE_ADMIN");
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
```

## 认证服务器配置

``` java
@EnableAuthorizationServer
@Configuration
public class AuthorizationServerConfiguration  extends AuthorizationServerConfigurerAdapter {

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.inMemory().withClient("client")
                .scopes("read","write")
                .secret("secret")
                .authorizedGrantTypes("authorization_code","password","implicit","client_credentials");}

    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        super.configure(security);
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
       endpoints.authenticationManager(authenticationManager);
    }

    @Autowired
    @Qualifier("authenticationManagerBean")
    private AuthenticationManager authenticationManager;
}

```

## 资源服务器配置

``` java
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableResourceServer
@Configuration
public class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {
	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.antMatcher("/oauth2/api/**").authorizeRequests()
			.antMatchers(HttpMethod.GET, "/read/**").access("#oauth2.hasScope('read')")
			.antMatchers(HttpMethod.POST, "/write/**").access("#oauth2.hasScope('write')")
			.antMatchers(HttpMethod.PUT, "/write/**").access("#oauth2.hasScope('write')")
			.antMatchers(HttpMethod.DELETE, "/write/**").access("#oauth2.hasScope('write')");
	}

}

```

## 资源服务器`filter-order`设置

需要在`application.yml`中将filter-order设置成3，具体原因参考 [链接](https://github.com/spring-projects/spring-boot/issues/5072)

## 防止cookie冲突
为了避免认证服务器的cookie和客户端的cookie冲突，出现错误，最好修改`cookie name` 或者设置`contextPath`。
# 测试
`postman`中提供OAuth 2.0的认证方式，可以获取到token之后再把认证加入http请求中，即可请求资源服务器的REST API

* 客户端信息

![输入图片说明](https://user-gold-cdn.xitu.io/2018/1/30/16145916dcaf9116?w=1222&h=1148&f=png&s=110518 "在这里输入图片标题")

* 授权

![输入图片说明](https://user-gold-cdn.xitu.io/2018/1/30/16145916e510f382?w=1600&h=1200&f=png&s=95458 "在这里输入图片标题")

* 获取的token

![输入图片说明](https://user-gold-cdn.xitu.io/2018/1/30/16145916dcd954aa?w=1584&h=802&f=png&s=71546 "在这里输入图片标题")



* 访问资源服务器API

![输入图片说明](https://user-gold-cdn.xitu.io/2018/1/30/16145916dc8c4347?w=2456&h=1424&f=png&s=308109 "在这里输入图片标题")




# 最后

测试代码[github地址](https://github.com/liuyatao/spring-security-oauth-sample)。有兴趣可以关注微信公众账号获取最新推送文章。

![输入图片说明](https://user-gold-cdn.xitu.io/2018/1/30/16145916dced2b4e?w=258&h=258&f=jpeg&s=26875 "在这里输入图片标题")
