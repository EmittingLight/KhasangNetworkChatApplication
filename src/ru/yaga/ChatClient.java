package ru.yaga;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatClient extends JFrame implements Runnable {

    private final Socket socket; // Сокет для соединения с сервером
    private final DataInputStream dataInputStream; // Поток ввода данных от сервера
    private final DataOutputStream dataOutputStream; // Поток вывода данных к серверу
    private final JTextArea outTextArea; // Область текста для вывода сообщений
    private final JTextField inTextField; // Поле для ввода сообщений пользователем
    private final String username; // Имя пользователя

    // Конструктор класса
    public ChatClient(Socket socket, DataInputStream dataInputStream, DataOutputStream dataOutputStream, String username) {
        super("Chat Client - " + username); // Установка заголовка окна
        this.socket = socket;
        this.dataInputStream = dataInputStream;
        this.dataOutputStream = dataOutputStream;
        this.username = username;

        // Настройка окна клиента
        setSize(400, 500); // Установка размеров окна
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Установка операции закрытия окна
        setLayout(new BorderLayout()); // Установка менеджера компоновки

        // Создание области текста и поля для ввода
        outTextArea = new JTextArea(); // Создание области текста
        add(outTextArea); // Добавление области текста в окно
        inTextField = new JTextField(); // Создание поля для ввода
        add(BorderLayout.SOUTH, inTextField); // Добавление поля для ввода внизу окна

        // Обработчик события закрытия окна
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                try {
                    dataOutputStream.close(); // Закрытие потока вывода данных
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    socket.close(); // Закрытие сокета
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        // Обработчик события ввода сообщения
        inTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String message = inTextField.getText(); // Получение текста из поля ввода
                    String formattedMessage = getCurrentDateTime() + " " + username + ": " + message; // Добавление даты и времени к сообщению
                    dataOutputStream.writeUTF(formattedMessage); // Отправка сообщения на сервер
                    dataOutputStream.flush(); // Принудительная очистка буфера вывода
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                inTextField.setText(""); // Очистка поля ввода после отправки сообщения
            }
        });

        setVisible(true); // Установка видимости окна
        inTextField.requestFocus(); // Установка фокуса на поле ввода
        new Thread(this).start(); // Создание и запуск нового потока
    }

    // Метод main
    public static void main(String[] args) {
        String site = "localhost"; // Адрес сервера
        String port = "8082"; // Порт сервера

        try {
            // Получение имени пользователя с помощью диалогового окна
            String username = JOptionPane.showInputDialog(null, "Введите имя пользователя:");

            Socket socket = new Socket(site, Integer.parseInt(port)); // Создание сокета
            DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream())); // Создание потока ввода данных
            DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream())); // Создание потока вывода данных

            dataOutputStream.writeUTF(username); // Отправка имени пользователя на сервер
            dataOutputStream.flush(); // Принудительная очистка буфера вывода

            new ChatClient(socket, dataInputStream, dataOutputStream, username); // Создание объекта клиента
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Переопределение метода run интерфейса Runnable
    @Override
    public void run() {
        try {
            while (true) {
                String line = dataInputStream.readUTF(); // Чтение сообщения от сервера
                String[] parts = line.split(" ", 2); // Разделение строки на две части: дату и сообщение
                if (parts.length == 2) {
                    String timestamp = parts[0]; // Получение временной метки
                    String message = parts[1]; // Получение только сообщения
                    outTextArea.append(timestamp + " " + message + "\n"); // Вывод сообщения в область текста
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            inTextField.setVisible(false); // Скрытие поля ввода
            validate(); // Перерисовка окна
        }
    }

    // Метод для получения текущего времени и даты
    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // Формат времени и даты
        Date now = new Date(); // Текущая дата и время
        return sdf.format(now); // Возвращаем отформатированную строку времени и даты
    }
}






