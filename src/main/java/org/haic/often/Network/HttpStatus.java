package org.haic.often.Network;

/**
 * Http状态码类,拓展了一些未收录的状态码
 *
 * @author haicdust
 * @version 1.0
 * @since 2022/2/13 17:55
 */
public class HttpStatus implements org.apache.http.HttpStatus {

	/**
	 * 状态码 425 - 服务器不愿意冒风险来处理该请求，原因是处理该请求可能会被“重放”，从而造成潜在的重放攻击
	 */
	public static final int SC_TOO_EARLY = 425;
	/**
	 * 状态码 426 - 一种HTTP协议的错误状态代码，表示服务器拒绝处理客户端使用当前协议发送的请求，但是可以接受其使用升级后的协议发送的请求 <br/>
	 * 服务器会在响应中使用 Upgrade (en-US) 首部来指定要求的协议。
	 */
	public static final int SC_UPGRADE_REQUIRED = 426;
	/**
	 * 状态码 428 - 服务器端要求发送条件请求 <br/>
	 * 一般的，这种情况意味着必要的条件首部——如 If-Match ——的缺失 <br/>
	 * 当一个条件首部的值不能匹配服务器端的状态的时候，应答的状态码应该是 412 Precondition Failed，前置条件验证失败
	 */
	public static final int SC_PRECONDITION_REQUIRED = 428;
	/**
	 * 状态码 429 - 在一定的时间内用户发送了太多的请求，即超出了“频次限制”<br/>
	 * 在响应中，可以提供一个  Retry-After 首部来提示用户需要等待多长时间之后再发送新的请求。
	 */
	public static final int SC_TOO_MANY_REQUEST = 429;
	/**
	 * 状态码 431 - 表示由于请求中的首部字段的值过大，服务器拒绝接受客户端的请求。客户端可以在缩减首部字段的体积后再次发送请求<br/>
	 * 该响应码可以用于首部总体体积过大的情况，也可以用于单个首部体积过大的情况。<br/>
	 * 这种错误不应该出现于经过良好测试的投入使用的系统当中，而是更多出现于测试新系统的时候
	 */
	public static final int SC_REQUEST_HEADER_FIELDS_TOO_LARGE = 431;
	/**
	 * 状态码 451 - （因法律原因不可用）是一种HTTP协议的错误状态代码，表示服务器由于法律原因，无法提供客户端请求的资源，例如可能会导致法律诉讼的页面。
	 */
	public static final int UNAVAILABLE_FOR_LEGAL_REASONS = 451;

}