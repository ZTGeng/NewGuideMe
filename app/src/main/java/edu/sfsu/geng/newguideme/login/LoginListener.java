package edu.sfsu.geng.newguideme.login;

/**
 * Created by Geng on 2016/11/20.
 */

public interface LoginListener {
//    void login(String email, String password);
    void login(String token, String username, String role, String rate, String inviteCode);
}
