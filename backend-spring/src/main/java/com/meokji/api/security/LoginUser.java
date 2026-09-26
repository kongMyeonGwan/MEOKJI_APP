package com.meokji.api.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러가 로그인한 회원을 받을 때 쓴다.
 *
 * <p>파이썬 쪽 세 가지 의존성을 하나로 합친 것이다.
 *
 * <pre>
 *   Depends(get_current_user)   -&gt;  &#64;LoginUser User me                      (없으면 401)
 *   Depends(get_optional_user)  -&gt;  &#64;LoginUser(required = false) User me    (없으면 null)
 *   Depends(get_admin_user)     -&gt;  &#64;LoginUser(admin = true) User me        (관리자 아니면 403)
 * </pre>
 *
 * <p>로그인이 필요한지를 <b>컨트롤러 메서드 자리에서</b> 정하는 것이 중요하다.
 * SecurityConfig 에 주소 목록을 또 적으면 라우터를 추가할 때 두 곳을 고쳐야 하고,
 * 한쪽만 고치면 조용히 열린다.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {

    /** false 면 토큰이 없어도 통과하고 null 이 들어온다 (게스트도 볼 수 있는 화면). */
    boolean required() default true;

    /** true 면 users.user_level = 1 만 통과한다. 아니면 403. */
    boolean admin() default false;
}
