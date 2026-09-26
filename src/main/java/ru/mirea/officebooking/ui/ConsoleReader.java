package ru.mirea.officebooking.ui;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import ru.mirea.officebooking.exception.ValidationException;

public class ConsoleReader {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private final Scanner scanner;

    public ConsoleReader(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public int readInt(String prompt) {
        while (true) {
            String line = readLine(prompt);
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: нужно ввести целое число (например, 5), а введено: «" + line + "»");
            }
        }
    }

    public int readIntInRange(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.printf("Ошибка: введите число от %d до %d%n", min, max);
        }
    }

    public long readId(String prompt) {
        while (true) {
            String line = readLine(prompt);
            try {
                long id = Long.parseLong(line);
                if (id > 0) {
                    return id;
                }
                System.out.println("Ошибка: id должен быть положительным числом");
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: id должен быть целым числом, а введено: «" + line + "»");
            }
        }
    }

    public Integer readOptionalPositiveInt(String prompt) {
        while (true) {
            String line = readLine(prompt);
            if (line.isEmpty()) {
                return null;
            }
            try {
                int value = Integer.parseInt(line);
                if (value > 0) {
                    return value;
                }
                System.out.println("Ошибка: число должно быть положительным или оставьте поле пустым");
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: нужно целое число или пустая строка, а введено: «" + line + "»");
            }
        }
    }

    public LocalDate readDate(String prompt) {
        while (true) {
            String line = readLine(prompt + " (гггг-мм-дд): ");
            try {
                return LocalDate.parse(line, DATE_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println("Ошибка: некорректная дата «" + line + "». Пример: 2026-09-20 (дата должна существовать)");
            }
        }
    }

    public LocalDateTime readDateTime(String prompt) {
        while (true) {
            String line = readLine(prompt + " (гггг-мм-дд чч:мм): ");
            try {
                return LocalDateTime.parse(line, DATE_TIME_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println("Ошибка: некорректные дата/время «" + line + "». Пример: 2026-09-20 14:00");
            }
        }
    }

    public boolean readYesNo(String prompt) {
        while (true) {
            String line = readLine(prompt + " (д/н): ").toLowerCase();
            switch (line) {
                case "д", "да", "y", "yes" -> {
                    return true;
                }
                case "н", "нет", "n", "no" -> {
                    return false;
                }
                default -> System.out.println("Ошибка: введите «д» или «н»");
            }
        }
    }

    public <T> T readChoice(String prompt, List<T> options) {
        while (true) {
            System.out.println(prompt);
            for (int i = 0; i < options.size(); i++) {
                System.out.printf("  %d. %s%n", i + 1, options.get(i));
            }
            String line = readLine("Выберите номер: ");
            try {
                int index = Integer.parseInt(line) - 1;
                if (index >= 0 && index < options.size()) {
                    return options.get(index);
                }
            } catch (NumberFormatException ignored) {
                System.out.print("");
            }
            System.out.printf("Ошибка: выберите номер от 1 до %d%n", options.size());
        }
    }

    public <E extends Enum<E>> E readEnum(Class<E> enumClass, String prompt) {
        return readChoice(prompt, Arrays.asList(enumClass.getEnumConstants()));
    }

    public <T> T readUntilValid(Supplier<T> source, UnaryOperator<T> validator) {
        while (true) {
            T value = source.get();
            try {
                return validator.apply(value);
            } catch (ValidationException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }
}
