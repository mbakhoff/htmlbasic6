# Using Spring Security

In the last tutorial we implemented a rather complicated security system for our web app.
All web apps should have proper security and the logic is very similar for all of them.
It would make sense to create a security library, so that the code is reused.

In fact, such library already exists in the Spring framework and it's called Spring Security.
In this tutorial we will replace our hand written security system with Spring Security.
Why didn't we use if from the start?
Because security is complicated and the best way to understand what's going on is to implement it ourself.
Why can't we keep using our own implementation?
Because it probably contains several subtle bugs that we don't even know about until we get hacked.

## Remove the hand written security:

* remove csrf tokens from html forms
* remove login tokens from the User class
* remove security filters
* remove cookie logic from login/logout
* (keep the password hash in the User)

## Enable Spring Security

Add this to *pom.xml*:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### Add a security configuration class

```java
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // TODO: configure the CSP header (doc link below)
    // HSTS, X-Frame-Options and CSRF tokens are enabled by default
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
```

[Spring Security Reference: Headers](https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#headers) has more information about configuring HTTP headers.

Spring Security has built in CSRF protection.
This is enabled by default and works out of the box.
Spring Security has a plugin for Thymeleaf that will automatically add CSRF tokens to all Thymeleaf html forms.
If you're interested, [Spring Security Reference: CSRF](https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#csrf) has more info.

### Add user account integration with Spring Security

```java
@Component
public class ForumUserService implements UserDetailsService {

  private static final Set<GrantedAuthority> DEFAULT_AUTHORITY = Collections.singleton(
      new SimpleGrantedAuthority("USER")
  );

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    // TODO: implement (in our system, email is the username)
    // should return org.springframework.security.core.userdetails.User
  }
}
```

Spring Security will take care of login tokens, checking password hashes, storing cookies etc.
The only thing it doesn't know how to do is store usernames and password hashes.
You must implement the `UserDetailsService` interface to help Spring out.

The `loadUserByUsername` method should return an user object that contains
* the username
* the password hash (the same BCrypt hash as we used before)
* a set of authorities (use the `DEFAULT_AUTHORITY` constant)

The authority system allows us to give different users different permissions in the system, such as admin/moderator/user permissions.
We currently don't have different types of users, so we'll give all users a generic *USER* authority and don't really use it anywhere.

### Change login and logout to use Spring Security

```java
@RequestMapping
void handleLogin(HttpServletRequest request) {
  request.login(username, password);
}

@RequestMapping
void handleLogout(HttpServletRequest request) {
  request.logout();
}

@RequestMapping
void doForumStuff(HttpServletRequest request) {
  String loggedInUser = request.getRemoteUser(); // username or null
  // do stuff with user
}
```

When you call the `login` method, Spring Security will look up the user information using the `UserDetailsService` we implemented before.
It will then hash the provided password and compare the result with the one stored in the `UserDetails` object.
If the password is ok, then Spring Security will remember the login and set the necessary cookies.

You no longer need to directly use the login token cookie and find the logged in user by the cookie.
After a successful login, the logged in user's username (in our case, the email) is available from the request object.

The [Spring Security Reference: Servlet API integration](https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#servletapi) describes all the methods that are available for the logged in user.

### Verify functionality

Make sure all the security features are still working:

* CSRF token is present
* CSP header is present
* HSTS header is present
* X-Frame-Options header is present
* login/logout works
* can only post if logged in

### How does this magic even work

When the server is starting up, Spring will scan all classes for `@Configuration`, `@Component` and `@WebApplicationInitializer` annotations.
Our `SecurityConfig` class has the `@EnableWebSecurity` annotation, which is a subclass of the `@Configuration` annotation.
The `@EnableWebSecurity` annotation tells Spring to automatically create the Spring Security filters (same thing we used in the last tutorial for CSRF and security headers).

When creating the filters, Spring Security will find all classes that extend the `WebSecurityConfigurerAdapter` class (such as our `SecurityConfig`) and call the `configure` method in them.
The configure methods tell Spring Security which security features to enable and provide the necessary details.

In the JPA repositories tutorial, we used dependency injection to have Spring inject the Spring Data repository implementations into our controllers:
```java
@Autowired
public SampleController(SampleItemRepository items) {
  this.items = items;
}
```

The component in Spring Security that deals with user login needs an implementation of the `UserDetailsService` interface to know the password hashes.
It will use the same dependency injection mechanism to find a suitable `UserDetailsService` implementation:
```java
// somewhere deep inside Spring Security; not the exact code, but idea is the same
class AuthenticationManager {

  @Autowired
  public AuthenticationManager(UserDetailsService users, PasswordEncoder hashAlgorithm) {
  }

  public void login(String username, String password) {
  }
}
```

Spring will search all classes that have the `@Component` annotation.
It will find our `ForumUserService` class, which implements `UserDetailsService`, create an instance of it and pass it to the `AuthenticationManager` constructor.

The authentication manager also needs a `PasswordEncoder` implementation that it will use to hash the passwords.
Spring will search all classes that have the `@Component` annotation, but we have no classes that implement `PasswordEncoder`.
Next, Spring will search all classes that have the `@Configuration` annotation and find our `SecurityConfig`.
Spring will search it and find the [*bean definition*](https://docs.spring.io/spring/docs/current/spring-framework-reference/htmlsingle/#beans-java-basic-concepts):
```java
@Bean
PasswordEncoder passwordEncoder() {
  return new BCryptPasswordEncoder();
}
```
The `@Bean` annotation and return type tell Spring it can use the method to create instances of `PasswordEncoder`.
Spring will call the method, get a new `BCryptPasswordEncoder` and pass it to the `AuthenticationManager` constructor.

# Webapp user preferences

In this part we're going to add a preferences page for our users.
The page will allow the user to change the display name and set the profile picture.

## Bootstrap it

Until now we've been writing custom CSS for each of our pages.
Unfortunately, not everyone is a designer.
[*Bootstrap*](https://getbootstrap.com/docs/3.3/css/) is a CSS library that makes a lot of common web site elements look ok.
Use the *bootstrap* library when building the preferences page.

Bootstrap checklist:
* include the bootstrap css by adding a `<link>` element to `<head>`.
  you can copy-paste the link from [Getting started - Bootstrap](https://getbootstrap.com/docs/3.3/getting-started/) (look for *Latest compiled and minified CSS*)
* whitelist `https://maxcdn.bootstrapcdn.com` in the CSP *style-src* directive; see [style-src at MDN](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/style-src) for details.
  if you don't, then the browser will refuse to load the bootstrap css (we configured script-src to 'self' earlier, which will only allow css files from our own server).
* don't invent your own page structure.
  base the structure on the samples in the [bootstrap documentation](https://getbootstrap.com/docs/3.3/css/)
  * wrap everything in the body in a `<div class="container">`
  * create the form as the samples show in the bootstrap *Forms* chapter

## Use Spring form validation

We're going to add validation to the forum user's details.
Spring validation can check the form fields that are submitted from the html page and automatically generate error messages for bad values.
Here is an example:

```java
class Person {
  @Min(18)
  int age;
}

@RequestMapping(method = RequestMethod.POST)
void handleForm(@Valid Person person, BindingResult bindingResult) {
  if (bindingResult.hasErrors()) {
    // send the user back to the edit form to fix the errors
    return "edit_form";
  }
  // save the form
  // send the user to the view form to see the result
  return "view_form";
}
```

```html
<form method="post">
  <input th:field="${person.age}" />
  <ul>
    <!-- if the age is under 18, Spring will generate an error and thymeleaf will render it here -->
    <li th:each="err : ${#fields.errors('person.age')}" th:text="${err}"></li>
  </ul>
  <input type="submit"/>
</form>
```

To enable validation, first add the `javax.validation` annotations to your model class's fields (e.g. Person's fields).
See the validator documentation for [examples](http://docs.jboss.org/hibernate/stable/validator/reference/en-US/html_single/#chapter-bean-constraints) and a [list of supported annotations](http://docs.jboss.org/hibernate/stable/validator/reference/en-US/html_single/#section-builtin-constraints) for details.

Next, add the model class as the request handler parameter.
For example, when you add `Person` as the request handler parameter, Spring will create a new `Person` object when a request arrives and sets all person's fields that it can find from the submitted form.
Annotate the parameter with `@Valid` to enable validation.
Note that the form input names must follow the naming scheme *modelClassName.fieldName* (see the SampleItem class and the sample html templates).
Finally, add the `BindingResult` parameter to the request handler.
The `BindingResult` parameter must be right after the model class parameter!
This object allows you to inspect the validation results.

All the validation results are also accessible in the Thymeleaf templates.
Thymeleaf has a [pretty decent documentation](http://www.thymeleaf.org/doc/tutorials/3.0/thymeleafspring.html#validation-and-error-messages) about that.
When the validation fails, you'll usually want to render the form again and let the user fix the errors.

### Add form validation to the preferences page

The display name should be at least 2 characters long.
Add the necessary annotation to the `User` class.
Change the request handler to validate the form.
If the validation fails, send the user back to the form and show the errors.

## Add profile icons to the users

Currently the users only have a display name.
We'll now add support for profile icons.
This consists of uploading the icons from the preferences page, storing the icons in the database and adding the icons to the forums posts.

### Prepare the database

We can store the icon in the User object.
The icon is just a piece of binary data.
Add a new field of type `byte[]` to the `User`.
Annotate the field with `@Lob` which tells Hibernate to store it and not complain.

### Add the file upload

File uploads work using the `<input type="file">` element.
However there is a trick - the form's content type must be switched from the default `application/x-www-form-urlencoded` to `multipart/form-data`.
Use the *enctype* attribute of the `<form>` element to do that.

What's up with the content type?
The browser and the server will automatically deal with it, you only need to add the *enctype* attribute to the `<form>`.
Nonetheless, it's good to know a little more about it.

The default `application/x-www-form-urlencoded` is rather primitive and cannot mix values from regular form inputs with data from the files.
It simply has no way to tell the server where the file begins and ends.
This is what a urlencoded *POST* looks like (based on *samples_edit.html*):

```text
POST /samples/urlencoded HTTP/1.1
Content-Type: application/x-www-form-urlencoded
Content-Length: 104

_csrf=f47290f2-c499-4e86-b9a2-8919bda7c889&key=some-key&value=some-value
```

The `multipart/form-data` is a bit more complicated and less efficient for sending form data, but it is file upload friendly.
This is what a multipart *POST* looks like:

```text
POST /samples/multipart HTTP/1.1
Content-Type: multipart/form-data; boundary=---------------------------9667853487481198227191294
Content-Length: 834

-----------------------------9667853487481198227191294
Content-Disposition: form-data; name="_csrf"

f47290f2-c499-4e86-b9a2-8919bda7c889
-----------------------------9667853487481198227191294
Content-Disposition: form-data; name="key"

some-key
-----------------------------9667853487481198227191294
Content-Disposition: form-data; name="value"

some-value
-----------------------------9667853487481198227191294
Content-Disposition: form-data; name="my-file-input"; filename="sample.txt"
Content-Type: text/plain

use wireshark to capture http packets

-----------------------------9667853487481198227191294--
```

### Save the file in the request handler

To access the posted file in the request handler, add a new parameter to the handler method: `@RequestParam MultipartFile theFile`.
Note that the parameter name must match the name of the html input, otherwise Spring cannot match them up.

Before saving the image, we should verify that it's an image and convert it to the *jpg* format.
```java
BufferedImage image = ImageIO.read(uploadedFile);
ImageIO.write(image, "jpg", output);
```

By default, the server will only allow small file uploads.
Override the max upload size in the *application.properties* file:
```text
spring.http.multipart.max-file-size=5MB
spring.http.multipart.max-request-size=5MB
```

### Add a request handler for accessing the image

The best way to show the users' icons in their posts would be to use the `<img>` tag.
The tag should contain the *src* attribute.. but where should it link to?
The image is in the database and the browser cannot directly access the database.

The solution is to add a new controller that deals with the users' icons.
The url scheme should be *GET /users/{id}/icon*.

To send the image as a HTTP response, use `ResponseEntity<byte[]>` as the request handler's return type.
Take a look at the static methods on the `ResponseEntity` class.
The response should:
* contain status code 200 OK
* set the *Content-Type* header to *image/jpeg* (hint: `MediaType.IMAGE_JPEG`)
* send the image's bytes as the response body

### Update the templates

Modify the forum templates to include the author's icon in each post.
Set the *max-height* and *max-width* css properties on the icons so that huge icons don't break the layout.
Add a default icon for users that don't have a custom icon.
