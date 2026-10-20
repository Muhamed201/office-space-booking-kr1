package ru.mirea.officebooking.fx;

import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.service.AccessPolicy;

public final class Session {

    private volatile Employee user;
    private Runnable logoutHandler = () -> {
    };

    public void login(Employee employee) {
        this.user = employee;
    }

    public void logout() {
        this.user = null;
        logoutHandler.run();
    }

    public void setLogoutHandler(Runnable handler) {
        this.logoutHandler = handler;
    }

    public Employee user() {
        Employee current = user;
        if (current == null) {
            throw new IllegalStateException("Пользователь не выполнил вход");
        }
        return current;
    }

    public long userId() {
        return user().getId();
    }

    public boolean isManager() {
        return AccessPolicy.isManager(user());
    }

    public boolean owns(long employeeId) {
        return userId() == employeeId;
    }
}
