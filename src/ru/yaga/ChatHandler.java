package ru.yaga;


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

    // Конструктор класса
    public ChatHandler(Socket socket) throws IOException {
        this.socket = socket;
        dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream())); // Создание потока ввода данных
        dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream())); // Создание потока вывода данных

        // Аутентификация пользователя
        this.username = dataInputStream.readUTF(); // Чтение имени пользователя от клиента
    }

    // Переопределенный метод run интерфейса Runnable
    @Override
    public void run() {
        handlers.add(this); // Добавление текущего обработчика в список
        try {
            while (true) {
                String message = dataInputStream.readUTF(); // Чтение сообщения от клиента
                broadcast(message); // Рассылка сообщения всем клиентам
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            handlers.remove(this); // Удаление текущего обработчика из списка
            try {
                dataOutputStream.close(); // Закрытие потока вывода данных
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                socket.close(); // Закрытие сокета
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
                        chatHandler.dataOutputStream.writeUTF(message); // Отправка сообщения клиенту
                    }
                    chatHandler.dataOutputStream.flush(); // Принудительная очистка буфера вывода
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
