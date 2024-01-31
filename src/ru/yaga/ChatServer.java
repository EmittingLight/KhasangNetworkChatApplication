package ru.yaga;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {
    public static void main(String[] args) {
        ServerSocket serverSocket = null; // Серверный сокет для прослушивания подключений
        try {
            serverSocket = new ServerSocket(8082); // Создание серверного сокета на порту 8082
            while (true) {
                Socket socket = serverSocket.accept(); // Принятие подключения от клиента
                System.out.println("Принято от: " + socket.getInetAddress()); // Вывод информации о подключившемся клиенте
                ChatHandler chatHandler = new ChatHandler(socket); // Создание обработчика чата для клиента
                chatHandler.start(); // Запуск потока обработчика чата
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
