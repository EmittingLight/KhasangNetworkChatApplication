package ru.yaga;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.JOptionPane;

public class ChatServer {
    private static final int MAX_USERS = 5;
    private static int connectedUsers = 0;

    public static synchronized void incrementConnectedUsers() {
        connectedUsers++;
    }

    public static synchronized void decrementConnectedUsers() {
        connectedUsers--;
    }

    public static synchronized int getConnectedUsers() {
        return connectedUsers;
    }

    public static void main(String[] args) {
        ServerSocket serverSocket = null; // Серверный сокет для прослушивания подключений
        try {
            serverSocket = new ServerSocket(8082); // Создание серверного сокета на порту 8082
            while (true) {
                Socket socket = serverSocket.accept(); // Принятие подключения от клиента

                if (getConnectedUsers() < MAX_USERS) {
                    incrementConnectedUsers();
                    System.out.println("Принято от: " + socket.getInetAddress()); // Вывод информации о подключившемся клиенте
                    ChatHandler chatHandler = new ChatHandler(socket); // Создание обработчика чата для клиента
                    chatHandler.start(); // Запуск потока обработчика чата
                } else {
                    JOptionPane.showMessageDialog(null, "Достигнуто максимальное количество пользователей (5).", "Предупреждение", JOptionPane.WARNING_MESSAGE);
                    socket.close(); // Закрываем сокет, так как максимальное количество пользователей достигнуто
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); // Вывод стека вызовов в случае исключения
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close(); // Закрытие серверного сокета
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

