package main;

import swyne.utils.AnalyzeTextUtil;

import javax.swing.*;

public class ConsoleWindow extends JFrame {
    JTextArea outputArea;
    JScrollPane outputPane;
    JTextArea inputField;
    JButton sendButton;

    public ConsoleWindow() {
        outputArea = new JTextArea("");
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setEditable(false);

        outputPane = new JScrollPane(outputArea);
        outputPane.setBounds(5, 5, 700, 490);
        add(outputPane);

        inputField = new JTextArea();
        inputField.setBounds(5, 500, 700, 50);
        add(inputField);

        sendButton = new JButton("Send");
        sendButton.setBounds(710, 505, 65, 30);
        add(sendButton);

        sendButton.addActionListener(e -> execute());

        setSize(800, 600);
        setLayout(null);
        setVisible(true);
    }
    private void execute() {
        //outputArea.setText(swyne.utils.AnalyzeTextUtil.testTransformations(inputField.getText()));
        StringBuilder temp = new StringBuilder();
        for(String w : AnalyzeTextUtil.splitIntoTokens(inputField.getText())) {
            temp.append(String.format("[%s] ", w));
        }
        outputArea.setText(temp.toString());
        inputField.setText("");
    }
}
