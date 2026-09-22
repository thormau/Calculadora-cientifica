package calculadora;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class CalculadoraM extends JFrame implements ActionListener {

    // Almacenes de datos independientes
    private double memory = 0.0;
    private double ans = 0.0;

    // Estado de la operación en curso
    private Double operand1 = null;
    private String pendingOperator = null;
    private boolean isNewEntry = true;
    private boolean hasError = false; // Indica si hay un error activo en pantalla
    private String historyExpr = "";

    // Componentes del visor
    private JLabel lblMemIndicator;
    private JLabel lblHistory;
    private JLabel lblDisplay;

    // Colores de texto del visor
    private final Color colorDisplayNormal = new Color(0xF0, 0xF0, 0xF0);
    private final Color colorDisplayError = new Color(0xFF, 0x52, 0x52);

    // Formateador a máximo 2 decimales
    private final DecimalFormat df = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));

    public CalculadoraM() {
        super("Calculadora V3.0");
        df.setGroupingUsed(false);
        df.setMaximumFractionDigits(2);
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(380, 560);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(new Color(0x18, 0x19, 0x1B));
        setLayout(new BorderLayout(10, 10));

        // --- PANEL SUPERIOR: VISOR Y DISPLAY ---
        JPanel displayPanel = new JPanel(new BorderLayout());
        displayPanel.setBackground(new Color(0x0E, 0x0F, 0x11));
        displayPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Renglón superior: Indicador M e Historial
        JPanel topInfoPanel = new JPanel(new BorderLayout());
        topInfoPanel.setOpaque(false);

        lblMemIndicator = new JLabel(" ");
        lblMemIndicator.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblMemIndicator.setForeground(new Color(0x8C, 0x91, 0x96));
        topInfoPanel.add(lblMemIndicator, BorderLayout.WEST);

        lblHistory = new JLabel("M = 0 | ANS = 0");
        lblHistory.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblHistory.setForeground(new Color(0x8C, 0x91, 0x96));
        lblHistory.setHorizontalAlignment(SwingConstants.RIGHT);
        topInfoPanel.add(lblHistory, BorderLayout.CENTER);

        displayPanel.add(topInfoPanel, BorderLayout.NORTH);

        // Display principal
        lblDisplay = new JLabel("0");
        lblDisplay.setFont(new Font("SansSerif", Font.BOLD, 38));
        lblDisplay.setForeground(colorDisplayNormal);
        lblDisplay.setHorizontalAlignment(SwingConstants.RIGHT);
        displayPanel.add(lblDisplay, BorderLayout.CENTER);

        JPanel displayContainer = new JPanel(new BorderLayout());
        displayContainer.setOpaque(false);
        displayContainer.setBorder(new EmptyBorder(12, 12, 0, 12));
        displayContainer.add(displayPanel, BorderLayout.CENTER);
        add(displayContainer, BorderLayout.NORTH);

        // --- PANEL DE BOTONES (4 Columnas x 6 Filas) ---
        JPanel buttonGrid = new JPanel(new GridLayout(6, 4, 8, 8));
        buttonGrid.setOpaque(false);
        buttonGrid.setBorder(new EmptyBorder(10, 12, 14, 12));

        String[][] buttons = {
            {"MC", "MR", "M+", "M-"},
            {"C", "CE", "ANS", "/"},
            {"7", "8", "9", "*"},
            {"4", "5", "6", "-"},
            {"1", "2", "3", "+"},
            {"0", ".", "+/-", "="}
        };

        for (String[] row : buttons) {
            for (String text : row) {
                JButton btn = createStyledButton(text);
                btn.addActionListener(this);
                buttonGrid.add(btn);
            }
        }

        add(buttonGrid, BorderLayout.CENTER);
    }

    private JButton createStyledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 17));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);

        Color bg;
        switch (text) {
            case "MC": case "MR": case "M+": case "M-":
            case "C": case "CE": case "ANS": case "/":
                bg = new Color(0xCD, 0x37, 0x37); // Rojo mate
                break;
            case "*": case "-": case "+": case "=":
                bg = new Color(0x2D, 0x41, 0x69); // Azul marino mate
                break;
            default:
                bg = new Color(0x3C, 0x42, 0x49); // Gris pizarra mate
                break;
        }
        btn.setBackground(bg);
        return btn;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();

        // Si la pantalla muestra error y se pulsa C o CE, resetea limpio
        if (hasError) {
            if (cmd.equals("C") || cmd.equals("CE")) {
                handleClearAll();
                updateUI();
                return;
            } else if (cmd.matches("[0-9]") || cmd.equals(".") || cmd.equals("ANS")) {
                // Al escribir un número nuevo o ANS, se limpia el error automáticamente
                resetErrorState();
            } else {
                // Si intenta presionar operadores mientras hay error, ignora la pulsación
                return;
            }
        }

        if (cmd.matches("[0-9]")) {
            handleDigit(cmd);
        } else if (cmd.equals(".")) {
            handleDecimal();
        } else if (cmd.equals("+/-")) {
            handleSignToggle();
        } else if (cmd.equals("+") || cmd.equals("-") || cmd.equals("*") || cmd.equals("/")) {
            handleOperator(cmd);
        } else if (cmd.equals("=")) {
            computeResult(false);
        } else if (cmd.equals("ANS")) {
            handleAns();
        } else if (cmd.equals("C")) {
            handleClearAll();
        } else if (cmd.equals("CE")) {
            handleClearEntry();
        } else if (cmd.equals("MC")) {
            handleMemoryClear();
        } else if (cmd.equals("MR")) {
            handleMemoryRecall();
        } else if (cmd.equals("M+")) {
            handleMemoryAdd();
        } else if (cmd.equals("M-")) {
            handleMemorySubtract();
        }

        updateUI();
    }

    // Restablece el color y estado después de un error
    private void resetErrorState() {
        hasError = false;
        lblDisplay.setForeground(colorDisplayNormal);
        lblDisplay.setText("0");
        operand1 = null;
        pendingOperator = null;
        historyExpr = "";
        isNewEntry = true;
    }

    private void handleDigit(String digit) {
        String current = lblDisplay.getText();
        if (isNewEntry) {
            lblDisplay.setText(digit);
            isNewEntry = false;
        } else {
            if (current.equals("0")) {
                lblDisplay.setText(digit);
            } else {
                lblDisplay.setText(current + digit);
            }
        }
    }

    private void handleDecimal() {
        String current = lblDisplay.getText();
        if (isNewEntry) {
            lblDisplay.setText("0.");
            isNewEntry = false;
        } else if (!current.contains(".")) {
            lblDisplay.setText(current + ".");
        }
    }

    private void handleSignToggle() {
        String current = lblDisplay.getText();
        if (!current.equals("0") && !current.equals("0.0")) {
            if (current.startsWith("-")) {
                lblDisplay.setText(current.substring(1));
            } else {
                lblDisplay.setText("-" + current);
            }
        }
    }

    private void handleOperator(String op) {
        double currentVal = parseDisplay();
        if (operand1 == null) {
            operand1 = currentVal;
        } else if (pendingOperator != null && !isNewEntry) {
            boolean err = computeResult(true);
            if (err) return;
            operand1 = parseDisplay();
        }
        pendingOperator = op;
        historyExpr = formatValue(operand1) + " " + op;
        isNewEntry = true;
    }

    private boolean computeResult(boolean isAuto) {
        if (operand1 == null || pendingOperator == null) return false;

        double op2 = parseDisplay();

        // Control de división entre cero: marca error pero permite recuperación inmediata
        if (pendingOperator.equals("/") && op2 == 0.0) {
            hasError = true;
            lblDisplay.setForeground(colorDisplayError);
            lblDisplay.setText("Error: División entre 0");
            operand1 = null;
            pendingOperator = null;
            historyExpr = "";
            isNewEntry = true;
            updateUI();
            return true;
        }

        double res = 0.0;
        switch (pendingOperator) {
            case "+": res = operand1 + op2; break;
            case "-": res = operand1 - op2; break;
            case "*": res = operand1 * op2; break;
            case "/": res = operand1 / op2; break;
        }

        ans = res;
        if (!isAuto) {
            historyExpr = formatValue(operand1) + " " + pendingOperator + " " + formatValue(op2);
            operand1 = null;
            pendingOperator = null;
        }
        lblDisplay.setText(formatValue(res));
        isNewEntry = true;
        return false;
    }

    private void handleAns() {
        lblDisplay.setText(formatValue(ans));
        isNewEntry = false;
    }

    private void handleClearEntry() {
        if (hasError) {
            resetErrorState();
        } else {
            lblDisplay.setText("0");
            isNewEntry = true;
        }
    }

    private void handleClearAll() {
        resetErrorState();
    }

    private void handleMemoryClear() {
        memory = 0.0;
    }

    private void handleMemoryRecall() {
        lblDisplay.setText(formatValue(memory));
        isNewEntry = true;
    }

    private void handleMemoryAdd() {
        memory += parseDisplay();
        isNewEntry = true;
    }

    private void handleMemorySubtract() {
        memory -= parseDisplay();
        isNewEntry = true;
    }

    private double parseDisplay() {
        try {
            return Double.parseDouble(lblDisplay.getText());
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private String formatValue(double val) {
        return df.format(val);
    }

    private void updateUI() {
        lblMemIndicator.setText(memory != 0.0 ? "M" : " ");
        String histText = "M = " + formatValue(memory) + " | ANS = " + formatValue(ans);
        if (!historyExpr.isEmpty()) {
            histText += " | " + historyExpr;
        }
        lblHistory.setText(histText);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new CalculadoraM().setVisible(true);
        });
    }
}