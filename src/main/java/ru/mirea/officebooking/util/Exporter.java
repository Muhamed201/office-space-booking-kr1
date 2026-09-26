package ru.mirea.officebooking.util;

import java.util.List;
import ru.mirea.officebooking.model.Booking;
import ru.mirea.officebooking.model.Employee;
import ru.mirea.officebooking.model.Workspace;

public interface Exporter {

    String formatName();

    String export(List<Employee> employees, List<Workspace> workspaces, List<Booking> bookings);
}
