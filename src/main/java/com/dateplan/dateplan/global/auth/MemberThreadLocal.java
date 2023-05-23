package com.dateplan.dateplan.global.auth;

import com.dateplan.dateplan.domain.member.entity.Member;

public class MemberThreadLocal {

	private static final ThreadLocal<Member> THREAD_LOCAL;

	static {
		THREAD_LOCAL = new ThreadLocal<>();
	}

	public static void set(Member member) {
		THREAD_LOCAL.set(member);
	}

	public static void remove() {
		THREAD_LOCAL.remove();
	}

	public static Member get() {
		return THREAD_LOCAL.get();
	}
}
