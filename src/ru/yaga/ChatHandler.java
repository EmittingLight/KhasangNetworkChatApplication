package ru.yaga;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ChatHandler extends Thread {
    private final Socket socket; // Сокет для соединения с клиентом
    DataInputStream dataInputStream; // Поток ввода данных от клиента
    DataOutputStream dataOutputStream; // Поток вывода данных к клиенту
    private static List<ChatHandler> handlers = Collections.synchronizedList(new ArrayList<>()); // Список обработчиков чатов
    private String username; // Имя пользователя

    private static final String FORBIDDEN_WORDS_FILE = "forbidden_words.txt";

    // Путь к файлу с именами пользователей
    private static final String USERS_FILE = "users.txt";
    // Путь к файлу с сообщениями
    private static final String MESSAGES_FILE = "messages.txt";

    // Конструктор класса
    public ChatHandler(Socket socket) throws IOException {
        this.socket = socket;
        dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream())); // Создание потока ввода данных
        dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream())); // Создание потока вывода данных

        // Аутентификация пользователя
        this.username = dataInputStream.readUTF(); // Чтение имени пользователя от клиента
        saveUsernameToFile(this.username); // Сохранение имени пользователя в файл
    }
    // Метод для проверки наличия запрещенных слов в сообщении
    private boolean hasForbiddenWords(String message) {
        try (BufferedReader reader = new BufferedReader(new FileReader(FORBIDDEN_WORDS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (message.toLowerCase().contains(line.toLowerCase())) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    // Переопределенный метод run интерфейса Runnable
    @Override
    public void run() {
        handlers.add(this);

        try {
            while (true) {
                String message = dataInputStream.readUTF();

                // Проверка наличия запрещенных слов
                if (hasForbiddenWords(message)) {
                    JOptionPane.showMessageDialog(null, "Ваше сообщение содержит запрещенные слова. Соединение будет разорвано.", "Предупреждение", JOptionPane.WARNING_MESSAGE);
                    break;
                }

                broadcast(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            handlers.remove(this);
            ChatServer.decrementConnectedUsers();
            try {
                dataOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Метод для рассылки сообщения всем клиентам
    private void broadcast(String message) {
        synchronized (handlers) {
            Iterator<ChatHandler> iterator = handlers.iterator();
            while (iterator.hasNext()) {
                ChatHandler chatHandler = iterator.next();
                try {
                    synchronized (chatHandler.dataOutputStream) {
                        // Отправка сообщения клиенту только если это не приватное сообщение
                        if (!message.startsWith("PRIVATE_MESSAGE")) {
                            chatHandler.dataOutputStream.writeUTF(message);
                            chatHandler.dataOutputStream.flush();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        saveMessageToFile(message); // Сохранение сообщения в файл

        /// Обработка приватных сообщений
        if (message.startsWith("PRIVATE_MESSAGE")) {
            String[] parts = message.split(":", 4);
            if (parts.length == 4) {
                String targetUser = parts[1];
                String privateMessage = parts[2] + ":" + parts[3];

                // Добавил проверку, чтобы избежать отправки приватного сообщения самому себе
                if (!targetUser.equals(username)) {
                    // Найти обработчика для целевого пользователя
                    synchronized (handlers) {
                        for (ChatHandler handler : handlers) {
                            if (handler.username.equals(targetUser)) {
                                try {
                                    synchronized (handler.dataOutputStream) {
                                        // Отправка приватного сообщения только целевому пользователю
                                        handler.dataOutputStream.writeUTF(privateMessage);
                                        handler.dataOutputStream.flush();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                        }
                    }
                } else {
                    // Окно с предупреждением когда пользователь пытается отправить приватное сообщение самому себе
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null, "Вы не можете отправить приватное сообщение самому себе.", "Предупреждение", JOptionPane.WARNING_MESSAGE);
                    });
                }
            }
        }
    }

    // Метод для сохранения имени пользователя в файл
    private void saveUsernameToFile(String username) {
        try (FileWriter fileWriter = new FileWriter(USERS_FILE, true);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
             PrintWriter printWriter = new PrintWriter(bufferedWriter)) {
            int userCount = getUserCount();
            printWriter.println((userCount + 1) + ":" + username);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Метод для получения текущего количества пользователей
    private int getUserCount() {
        int userCount = 0;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(USERS_FILE))) {
            while (bufferedReader.readLine() != null) {
                userCount++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return userCount;
    }

    // Метод для сохранения сообщения в файл
    private void saveMessageToFile(String message) {
        try (FileWriter fileWriter = new FileWriter(MESSAGES_FILE, true);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
             PrintWriter printWriter = new PrintWriter(bufferedWriter)) {
            printWriter.println(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

