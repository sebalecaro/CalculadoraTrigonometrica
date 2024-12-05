package com.example.calculadoratrigonometricafinal;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private EditText sideAInput, sideBInput, sideCInput, angleAInput, angleBInput, angleCInput;
    private TextView resultDisplay;
    private Button calculateButton, refreshButton;
    private DecimalFormat df;
    private static final double EPSILON = 0.0001;
    private CardView resultCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeViews();
        setupListeners();
        df = new DecimalFormat("#.####");
    }

    private void initializeViews() {
        sideAInput = findViewById(R.id.sideAInput);
        sideBInput = findViewById(R.id.sideBInput);
        sideCInput = findViewById(R.id.sideCInput);
        angleAInput = findViewById(R.id.angleAInput);
        angleBInput = findViewById(R.id.angleBInput);
        angleCInput = findViewById(R.id.angleCInput);
        resultDisplay = findViewById(R.id.resultDisplay);
        calculateButton = findViewById(R.id.calculateButton);
        refreshButton = findViewById(R.id.refreshButton);
        resultCard = findViewById(R.id.resultCard);
    }

    private void setupListeners() {
        calculateButton.setOnClickListener(v -> calculateTriangle());
        refreshButton.setOnClickListener(v -> resetInputs());

        EditText[] angleInputs = {angleAInput, angleBInput, angleCInput};
        for (EditText angleInput : angleInputs) {
            angleInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    validateAngleInput((EditText) v);
                }
            });
        }
    }

    private void validateAngleInput(EditText angleInput) {
        String value = angleInput.getText().toString();
        if (!TextUtils.isEmpty(value)) {
            double angle = Double.parseDouble(value);
            if (angle <= 0 || angle >= 180) {
                angleInput.setError(getString(R.string.error_angle_range));
                angleInput.setText("");
            }

            // Validar la suma con los otros ángulos
            double sumAngles = angle;
            if (!angleAInput.getText().toString().isEmpty() && angleInput != angleAInput) {
                sumAngles += Double.parseDouble(angleAInput.getText().toString());
            }
            if (!angleBInput.getText().toString().isEmpty() && angleInput != angleBInput) {
                sumAngles += Double.parseDouble(angleBInput.getText().toString());
            }
            if (!angleCInput.getText().toString().isEmpty() && angleInput != angleCInput) {
                sumAngles += Double.parseDouble(angleCInput.getText().toString());
            }

            if (sumAngles > 180) {
                angleInput.setError(getString(R.string.error_angles_sum));
                angleInput.setText("");
            }
        }
    }

    private void resetInputs() {
        EditText[] inputs = {sideAInput, sideBInput, sideCInput, angleAInput, angleBInput, angleCInput};
        for (EditText input : inputs) {
            input.setText("");
            input.setError(null);
        }
        resultDisplay.setText("Ingrese los valores conocidos del triángulo.");
        resultCard.setVisibility(android.view.View.GONE);
    }

    private Double getInputValue(EditText input) {
        String value = input.getText().toString();
        return TextUtils.isEmpty(value) ? null : Double.parseDouble(value);
    }

    private boolean validateTriangleData(TriangleData triangle) {
        if (triangle.getKnownValuesCount() < 3) {
            Toast.makeText(this, "Se necesitan al menos 3 valores para resolver el triángulo",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        // Verificar la suma de ángulos conocidos
        double sumAngles = 0;
        int knownAngles = 0;

        if (triangle.angleA != null) {
            sumAngles += triangle.angleA;
            knownAngles++;
        }
        if (triangle.angleB != null) {
            sumAngles += triangle.angleB;
            knownAngles++;
        }
        if (triangle.angleC != null) {
            sumAngles += triangle.angleC;
            knownAngles++;
        }

        // Si tenemos 3 ángulos, la suma debe ser exactamente 180
        if (knownAngles == 3 && Math.abs(sumAngles - 180) > EPSILON) {
            Toast.makeText(this, "Los ángulos internos de un triángulo no pueden sumar más de 180°",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        // Si tenemos 2 ángulos, la suma no debe exceder 180
        if (knownAngles == 2 && sumAngles >= 180) {
            Toast.makeText(this, "Los ángulos proporcionados no pueden formar un triángulo válido",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    private boolean isValidTriangle(TriangleData triangle) {
        if (triangle.allSidesKnown()) {
            return (triangle.sideA + triangle.sideB > triangle.sideC) &&
                    (triangle.sideB + triangle.sideC > triangle.sideA) &&
                    (triangle.sideC + triangle.sideA > triangle.sideB);
        }
        return true;
    }

    private void displayResults(List<String> results, String additionalInfo) {
        StringBuilder sb = new StringBuilder();
        for (String result : results) {
            sb.append(result).append("\n");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            if (!additionalInfo.isEmpty()) {
                sb.append("\n").append(additionalInfo);
            }
        }
        resultDisplay.setText(sb.toString());
    }

    private void calculateTriangle() {
        try {
            TriangleData triangle = new TriangleData(
                    getInputValue(sideAInput),
                    getInputValue(sideBInput),
                    getInputValue(sideCInput),
                    getInputValue(angleAInput),
                    getInputValue(angleBInput),
                    getInputValue(angleCInput)
            );

            if (!validateTriangleData(triangle)) {
                return;
            }

            List<String> results = new ArrayList<>();
            resolveAdvancedCases(triangle, results);

            if (!isValidTriangle(triangle)) {
                Toast.makeText(this, "Los valores proporcionados no forman un triángulo válido",
                        Toast.LENGTH_LONG).show();
                return;
            }

            displayResults(results, "");
            resultCard.setVisibility(android.view.View.VISIBLE);

        } catch (Exception e) {
            Toast.makeText(this, "Error en el cálculo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public class TriangleData {
        Double sideA, sideB, sideC;
        Double angleA, angleB, angleC;
        Double hypotenuse;
        private static final double EPSILON = 0.0001;

        public TriangleData(Double sideA, Double sideB, Double sideC,
                            Double angleA, Double angleB, Double angleC) {
            this.sideA = sideA;
            this.sideB = sideB;
            this.sideC = sideC;
            this.angleA = angleA;
            this.angleB = angleB;
            this.angleC = angleC;
        }

        public boolean hasRightAngle() {
            return (angleA != null && Math.abs(angleA - 90) < EPSILON) ||
                    (angleB != null && Math.abs(angleB - 90) < EPSILON) ||
                    (angleC != null && Math.abs(angleC - 90) < EPSILON);
        }

        public boolean allSidesKnown() {
            return sideA != null && sideB != null && sideC != null;
        }

        public boolean allAnglesKnown() {
            return angleA != null && angleB != null && angleC != null;
        }

        public int getKnownSidesCount() {
            int count = 0;
            if (sideA != null) count++;
            if (sideB != null) count++;
            if (sideC != null) count++;
            return count;
        }

        public int getKnownAnglesCount() {
            int count = 0;
            if (angleA != null) count++;
            if (angleB != null) count++;
            if (angleC != null) count++;
            return count;
        }

        public int getKnownValuesCount() {
            return getKnownSidesCount() + getKnownAnglesCount();
        }

        public boolean canUseCosineRule() {
            return (sideA != null && sideB != null && angleC != null) ||
                    (sideB != null && sideC != null && angleA != null) ||
                    (sideA != null && sideC != null && angleB != null);
        }
    }

    private void resolveAdvancedCases(TriangleData triangle, List<String> results) {
        boolean changesMade;
        int iterations = 0;
        final int MAX_ITERATIONS = 3;

        do {
            changesMade = false;
            iterations++;

            if (triangle.getKnownAnglesCount() == 2 && !triangle.allAnglesKnown()) {
                resolveBasicAngles(triangle, results);
                changesMade = true;
            }

            if (triangle.hasRightAngle()) {
                boolean previousState = Boolean.parseBoolean(getTriangleState(triangle));
                resolveRightTriangle(triangle, results);
                if (!previousState) {
                    changesMade = true;
                }
            }

            if (!triangle.allSidesKnown() && triangle.canUseCosineRule()) {
                boolean previousState = Boolean.parseBoolean(getTriangleState(triangle));
                calculateSideWithCosineRule(triangle, results);
                if (!previousState) {
                    changesMade = true;
                }
            }

            if (triangle.allSidesKnown() && !triangle.allAnglesKnown()) {
                boolean previousState = Boolean.parseBoolean(getTriangleState(triangle));
                calculateAnglesWithCosineRule(triangle, results);
                if (!previousState) {
                    changesMade = true;
                }
            }

            if (triangle.getKnownSidesCount() >= 1 && triangle.getKnownAnglesCount() >= 1) {
                boolean previousState = Boolean.parseBoolean(getTriangleState(triangle));
                calculateWithLawOfSines(triangle, results);
                if (!previousState) {
                    changesMade = true;
                }
            }

        } while (changesMade && iterations < MAX_ITERATIONS &&
                (!triangle.allSidesKnown() || !triangle.allAnglesKnown()));
    }

    private String getTriangleState(TriangleData triangle) {
        return String.format("%s,%s,%s,%s,%s,%s",
                triangle.sideA, triangle.sideB, triangle.sideC,
                triangle.angleA, triangle.angleB, triangle.angleC);
    }

    private void resolveBasicAngles(TriangleData triangle, List<String> results) {
        Double missingAngle = 180.0;
        if (triangle.angleA != null) missingAngle -= triangle.angleA;
        if (triangle.angleB != null) missingAngle -= triangle.angleB;
        if (triangle.angleC != null) missingAngle -= triangle.angleC;

        if (triangle.angleA == null) {
            triangle.angleA = missingAngle;
            results.add("Ángulo A = " + df.format(missingAngle) + "° (Suma de ángulos internos = 180°)");
        } else if (triangle.angleB == null) {
            triangle.angleB = missingAngle;
            results.add("Ángulo B = " + df.format(missingAngle) + "° (Suma de ángulos internos = 180°)");
        } else if (triangle.angleC == null) {
            triangle.angleC = missingAngle;
            results.add("Ángulo C = " + df.format(missingAngle) + "° (Suma de ángulos internos = 180°)");
        }
    }

    private double calculateAngleWithCosineRule(double adjacent1, double adjacent2, double opposite) {
        double cosAngle = (Math.pow(adjacent1, 2) + Math.pow(adjacent2, 2) - Math.pow(opposite, 2)) /
                (2 * adjacent1 * adjacent2);
        return Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cosAngle))));
    }

    private void resolveRightTriangle(TriangleData triangle, List<String> results) {
        identifyHypotenuse(triangle);

        if (!triangle.allSidesKnown()) {
            calculatePythagoreanTheorem(triangle, results);
        }

        if (!triangle.allAnglesKnown()) {
            calculateRightTriangleAngles(triangle, results);
        }
    }

    private void identifyHypotenuse(TriangleData triangle) {
        if (triangle.angleA != null && Math.abs(triangle.angleA - 90) < EPSILON) {
            triangle.hypotenuse = triangle.sideA;
        } else if (triangle.angleB != null && Math.abs(triangle.angleB - 90) < EPSILON) {
            triangle.hypotenuse = triangle.sideB;
        } else if (triangle.angleC != null && Math.abs(triangle.angleC - 90) < EPSILON) {
            triangle.hypotenuse = triangle.sideC;
        }
    }

    private void calculatePythagoreanTheorem(TriangleData triangle, List<String> results) {
        if (triangle.sideA != null && triangle.sideB != null && triangle.sideC == null) {
            triangle.sideC = Math.sqrt(Math.pow(triangle.sideA, 2) + Math.pow(triangle.sideB, 2));
            results.add("Lado c = " + df.format(triangle.sideC) + " (Teorema de Pitágoras)");
        } else if (triangle.sideA != null && triangle.sideC != null && triangle.sideB == null) {
            triangle.sideB = Math.sqrt(Math.pow(triangle.sideC, 2) - Math.pow(triangle.sideA, 2));
            results.add("Lado b = " + df.format(triangle.sideB) + " (Teorema de Pitágoras)");
        } else if (triangle.sideB != null && triangle.sideC != null && triangle.sideA == null) {
            triangle.sideA = Math.sqrt(Math.pow(triangle.sideC, 2) - Math.pow(triangle.sideB, 2));
            results.add("Lado a = " + df.format(triangle.sideA) + " (Teorema de Pitágoras)");
        }
    }

    private void calculateRightTriangleAngles(TriangleData triangle, List<String> results) {
        if (triangle.sideA != null && triangle.sideC != null) {
            if (triangle.angleA == null) {
                triangle.angleA = Math.toDegrees(Math.asin(triangle.sideA / triangle.sideC));
                results.add("Ángulo A = " + df.format(triangle.angleA) + "° (Función arcoseno)");
            }
        }
        if (triangle.sideB != null && triangle.sideC != null) {
            if (triangle.angleB == null) {
                triangle.angleB = Math.toDegrees(Math.asin(triangle.sideB / triangle.sideC));
                results.add("Ángulo B = " + df.format(triangle.angleB) + "° (Función arcoseno)");
            }
        }
    }

    private void calculateAnglesWithCosineRule(TriangleData triangle, List<String> results) {
        if (triangle.angleA == null) {
            triangle.angleA = calculateAngleWithCosineRule(triangle.sideB, triangle.sideC, triangle.sideA);
            results.add("Ángulo A = " + df.format(triangle.angleA) + "° (Ley de Cosenos)");
        }
        if (triangle.angleB == null) {
            triangle.angleB = calculateAngleWithCosineRule(triangle.sideA, triangle.sideC, triangle.sideB);
            results.add("Ángulo B = " + df.format(triangle.angleB) + "° (Ley de Cosenos)");
        }
        if (triangle.angleC == null) {
            triangle.angleC = calculateAngleWithCosineRule(triangle.sideA, triangle.sideB, triangle.sideC);
            results.add("Ángulo C = " + df.format(triangle.angleC) + "° (Ley de Cosenos)");
        }
    }

    private void calculateSideWithCosineRule(TriangleData triangle, List<String> results) {
        // Calcular lado C
        if (triangle.sideC == null && triangle.sideA != null && triangle.sideB != null && triangle.angleC != null) {
            triangle.sideC = Math.sqrt(Math.pow(triangle.sideA, 2) + Math.pow(triangle.sideB, 2) -
                    2 * triangle.sideA * triangle.sideB * Math.cos(Math.toRadians(triangle.angleC)));
            results.add("Lado c = " + df.format(triangle.sideC) + " (Ley de Cosenos)");
        }

        // Calcular lado A
        if (triangle.sideA == null && triangle.sideB != null && triangle.sideC != null && triangle.angleA != null) {
            triangle.sideA = Math.sqrt(Math.pow(triangle.sideB, 2) + Math.pow(triangle.sideC, 2) -
                    2 * triangle.sideB * triangle.sideC * Math.cos(Math.toRadians(triangle.angleA)));
            results.add("Lado a = " + df.format(triangle.sideA) + " (Ley de Cosenos)");
        }

        // Calcular lado B
        if (triangle.sideB == null && triangle.sideA != null && triangle.sideC != null && triangle.angleB != null) {
            triangle.sideB = Math.sqrt(Math.pow(triangle.sideA, 2) + Math.pow(triangle.sideC, 2) -
                    2 * triangle.sideA * triangle.sideC * Math.cos(Math.toRadians(triangle.angleB)));
            results.add("Lado b = " + df.format(triangle.sideB) + " (Ley de Cosenos)");
        }
    }

    private void calculateWithLawOfSines(TriangleData triangle, List<String> results) {
        double ratio = -1;

        // Calcular la razón seno inicial si es posible
        if (triangle.sideA != null && triangle.angleA != null) {
            ratio = triangle.sideA / Math.sin(Math.toRadians(triangle.angleA));
        } else if (triangle.sideB != null && triangle.angleB != null) {
            ratio = triangle.sideB / Math.sin(Math.toRadians(triangle.angleB));
        } else if (triangle.sideC != null && triangle.angleC != null) {
            ratio = triangle.sideC / Math.sin(Math.toRadians(triangle.angleC));
        }

        if (ratio > 0) {
            // Calcular lados faltantes usando la ley de senos
            if (triangle.sideA == null && triangle.angleA != null) {
                triangle.sideA = ratio * Math.sin(Math.toRadians(triangle.angleA));
                results.add("Lado a = " + df.format(triangle.sideA) + " (Ley de Senos)");
            }
            if (triangle.sideB == null && triangle.angleB != null) {
                triangle.sideB = ratio * Math.sin(Math.toRadians(triangle.angleB));
                results.add("Lado b = " + df.format(triangle.sideB) + " (Ley de Senos)");
            }
            if (triangle.sideC == null && triangle.angleC != null) {
                triangle.sideC = ratio * Math.sin(Math.toRadians(triangle.angleC));
                results.add("Lado c = " + df.format(triangle.sideC) + " (Ley de Senos)");
            }

            // Calcular ángulos faltantes usando la ley de senos
            if (triangle.angleA == null && triangle.sideA != null) {
                triangle.angleA = Math.toDegrees(Math.asin(triangle.sideA / ratio));
                results.add("Ángulo A = " + df.format(triangle.angleA) + "° (Ley de Senos)");
            }
            if (triangle.angleB == null && triangle.sideB != null) {
                triangle.angleB = Math.toDegrees(Math.asin(triangle.sideB / ratio));
                results.add("Ángulo B = " + df.format(triangle.angleB) + "° (Ley de Senos)");
            }
            if (triangle.angleC == null && triangle.sideC != null) {
                triangle.angleC = Math.toDegrees(Math.asin(triangle.sideC / ratio));
                results.add("Ángulo C = " + df.format(triangle.angleC) + "° (Ley de Senos)");
            }
        }
    }
}