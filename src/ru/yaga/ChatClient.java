package ru.yaga;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class ChatClient extends JFrame {
    private final Socket socket;
    private final DataInputStream dataInputStream;
    private final DataOutputStream dataOutputStream;
    private final JTextPane outTextPane;
    private final JTextField inTextField;
    private final String username;
    private static final String USERS_FILE = "users.txt";
    private JComboBox<String> userComboBox;

    // Конструктор класса
    public ChatClient(Socket socket, DataInputStream dataInputStream, DataOutputStream dataOutputStream, String username) {
        super("Chat Client - " + username);
        this.socket = socket;
        this.dataInputStream = dataInputStream;
        this.dataOutputStream = dataOutputStream;
        this.username = username;

        setSize(400, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Создание выпадающего списка пользователей
        this.userComboBox = new JComboBox<>();
        add(BorderLayout.NORTH, this.userComboBox);

        // Обработчик события выбора пользователя из списка
        this.userComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedUser = (String) userComboBox.getSelectedItem();
            }
        });

        // Отображение списка пользователей в выпадающем списке
        displayUserList(this.userComboBox);

        // Добавляем обработчик события закрытия окна
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Вызываем метод удаления пользователя из файла при закрытии окна
                removeUserFromFile(username, userComboBox);
                super.windowClosing(e);
            }
        });

        // Вызов метода для обновления списка пользователей
        updateUserList(this.userComboBox);

        // Создание области вывода сообщений
        outTextPane = new JTextPane();
        outTextPane.setEditable(false);
        add(new JScrollPane(outTextPane));

        // Создание поля ввода сообщений
        inTextField = new JTextField();
        add(BorderLayout.SOUTH, inTextField);

        // Обработчик события ввода текста пользователем
        inTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // Получение текста сообщения и его форматирование
                    String message = inTextField.getText();
                    String formattedMessage = getCurrentDateTime() + " " + username + ": " + message;

                    // Отправка сообщения на сервер
                    dataOutputStream.writeUTF(formattedMessage);
                    dataOutputStream.flush();

                    // Очистка поля ввода
                    inTextField.setText("");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        // Установка видимости окна
        setVisible(true);
        // Установка фокуса на поле ввода
        inTextField.requestFocus();

        // Запуск фонового потока для чтения сообщений от сервера
        new ChatWorker().execute();

        // Вызов метода для запуска потока обновления списка пользователей
        startUserListUpdater(userComboBox);
    }

    // Метод для отображения выпадающего списка пользователей
    private void displayUserList(JComboBox<String> userComboBox) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(USERS_FILE))) {
            List<String> userList = new ArrayList<>();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                userList.add(line.split(":")[1]); // Получение имени пользователя из строки
            }

            // Отображение списка пользователей в выпадающем списке
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(userList.toArray(new String[0]));
            userComboBox.setModel(model);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Метод для обновления списка пользователей
    private void updateUserList(JComboBox<String> userComboBox) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(USERS_FILE))) {
            List<String> userList = new ArrayList<>();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                userList.add(line.split(":")[1]); // Получение имени пользователя из строки
            }

            // Обновление списка в выпадающем списке
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(userList.toArray(new String[0]));
            userComboBox.setModel(model);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Метод для запуска потока обновления списка пользователей
    private void startUserListUpdater(JComboBox<String> userComboBox) {
        new Thread(() -> {
            while (true) {
                try {
                    // Задержка перед обновлением списка (каждые 10 секунд)
                    Thread.sleep(10000);

                    // Обновление списка пользователей
                    updateUserList(userComboBox);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //Метод, выполняющий фоновые операции, читая данные от сервера в бесконечном цикле.
    private class ChatWorker extends SwingWorker<Void, String> {
        // В методе ChatWorker, после блока finally
        @Override
        protected Void doInBackground() {
            try {
                while (true) {
                    // Чтение строки от сервера и публикация для отображения в главном потоке
                    String line = dataInputStream.readUTF().trim();
                    publish(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Скрытие поля ввода, перерисовка окна и удаление пользователя из файла при завершении
                inTextField.setVisible(false);
                validate();
                removeUserFromFile(username, userComboBox);

                // Закрытие сокета и завершение приложения
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }
            return null;
        }

        //Метод, обрабатывающий и отображающий полученные строки в главном потоке.
        @Override
        protected void process(java.util.List<String> chunks) {
            for (String line : chunks) {
                appendStyledMessage(line);
            }
        }
    }

    //Метод для добавления стилизованного сообщения в область вывода сообщений.
    private void appendStyledMessage(String message) {
        StyledDocument doc = outTextPane.getStyledDocument();

        // Создание стилей для разных компонентов сообщения
        Style dateStyle = doc.addStyle("DateStyle", null);
        Style timeStyle = doc.addStyle("TimeStyle", null);
        Style usernameStyle = doc.addStyle("UsernameStyle", null);
        Style textStyle = doc.addStyle("TextStyle", null);

        // Разделение сообщения на части: дату, время, имя пользователя и текст
        String[] parts = message.split(" ", 3);
        if (parts.length == 3) {
            String date = parts[0];
            String time = parts[1];
            String username = parts[2].substring(0, parts[2].indexOf(":"));
            String text = parts[2].substring(parts[2].indexOf(":") + 2);

            // Установка цвета для каждого компонента сообщения
            StyleConstants.setForeground(dateStyle, Color.BLUE);
            StyleConstants.setForeground(timeStyle, Color.GREEN);
            StyleConstants.setForeground(usernameStyle, Color.RED);
            StyleConstants.setForeground(textStyle, Color.BLACK);

            try {
                // Вставка каждой компоненты сообщения в область вывода
                doc.insertString(doc.getLength(), date + " ", dateStyle);
                doc.insertString(doc.getLength(), time + " ", timeStyle);
                doc.insertString(doc.getLength(), username, usernameStyle);
                doc.insertString(doc.getLength(), ": " + text + "\n", textStyle);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    //Метод для получения текущей даты и времени в формате "yyyy-MM-dd HH:mm:ss".
    private String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        return sdf.format(now);
    }

    //Точка входа в приложение.
    public static void main(String[] args) {
        String site = "localhost";
        String port = "8082";

        try {
            // Получение имени пользователя с помощью диалогового окна
            String username = JOptionPane.showInputDialog(null, "Введите имя пользователя:");

            // Создание сокета и потоков ввода/вывода данных
            Socket socket = new Socket(site, Integer.parseInt(port));
            DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            // Отправка имени пользователя на сервер
            dataOutputStream.writeUTF(username);
            dataOutputStream.flush();

            // Создание объекта клиента
            new ChatClient(socket, dataInputStream, dataOutputStream, username);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Метод для удаления пользователя из файла
    private void removeUserFromFile(String username, JComboBox<String> userComboBox) {
        try {
            // Создание временного файла
            File tempFile = new File("temp_users.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

            BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE));
            String line;
            while ((line = reader.readLine()) != null) {
                // Если строка содержит имя пользователя, пропускаем ее
                if (line.contains(username)) {
                    continue;
                }
                // Записываем остальные строки во временный файл
                writer.write(line + "\n");
            }
            reader.close();
            writer.close();

            // Копирование содержимого временного файла в исходный файл
            try (FileInputStream inputStream = new FileInputStream(tempFile);
                 FileOutputStream outputStream = new FileOutputStream(USERS_FILE)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }

            // Удаление временного файла
            if (!tempFile.delete()) {
                System.out.println("Не удалось удалить временный файл");
            }

            // Обновление списка пользователей в выпадающем списке
            displayUserList(userComboBox);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


