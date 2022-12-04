package org.example;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;


public class Main {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    static List<Question> wrongAnsweredQuestions = new ArrayList<>();
    static long numberQuestionsPerSession = 10;

    /**
     * 80 -> 50min
     * numberQuestions -> x
     * x =  numberQuestions *  5/8
     **/
    public static void main(String[] args) throws Exception {
        Scanner in = new Scanner(System.in);
        List<Question> questions = parseFile();
//        Collections.shuffle(questions);

        while (questions.size() / numberQuestionsPerSession > 18) removeAnsweredQuestions(questions);

        while (!questions.isEmpty()) {
            long remainingSessions = (questions.size() + numberQuestionsPerSession - 1) / numberQuestionsPerSession;

            print(ANSI_RED, "=====================================");
            print(ANSI_RED, "Remaining Sessions: " + remainingSessions);
            print(ANSI_RED, "Remaining questions: " + questions.size());
            print(ANSI_RED, "Questions per session: " + numberQuestionsPerSession);
            print(ANSI_RED, "Start Next Session ? (y/n)");
            print(ANSI_RED, "=====================================");

            String input;
            do {
                input = in.nextLine().toLowerCase();
            } while (!input.equals("y") && !input.equals("n"));
            if (input.equals("n")) break;

            execSession(questions);
            removeAnsweredQuestions(questions);
        }
    }

    private static void removeAnsweredQuestions(List<Question> questions) {
        long counter = numberQuestionsPerSession;
        while (!questions.isEmpty() && counter > 0) {
            questions.remove(0);
            counter--;
        }
    }

    private static void execSession(List<Question> questions) {
        Scanner in = new Scanner(System.in);

        numberQuestionsPerSession = Math.min(numberQuestionsPerSession, questions.size());

        int correctAnswers = 0;
        long end = (long) (System.currentTimeMillis() + (numberQuestionsPerSession * 5.0 / 8.0) * 60_000); // end = now + 50min

        for (int i = 0; i < numberQuestionsPerSession; i++) {
            displayInfo(end, i);

            Question question = questions.get(i);
            printQuestion(question);

            // check for command
            String input = in.next();
            if (input.equals("exist")) {
                break;
            } else if (input.equals("skip")) {
                questions.remove(i);
                i--;
                continue;
            }

            char[] answer = input.toCharArray();

            if (System.currentTimeMillis() > end) {
                print(ANSI_RED, "Time's up!");
                wrongAnsweredQuestions.add(question);
                break;
            }

            if (question.isValidAnswer(answer)) {
                correctAnswers++;
            } else {
                wrongAnsweredQuestions.add(question);
            }
        }

        displayScore(correctAnswers, end);

        displayWrongAnsweredQuestions();


    }

    private static void displayScore(int correctAnswers, long end) {
        long r = (end - System.currentTimeMillis()) / 1000;
        long m = (r % 3600) / 60;
        long s = r % 60;
        String info = String.format("==========================================\n" +
                        "Score: [%d%%] | remaining Time: [%dm:%ds]" +
                        "\n==========================================",
                (correctAnswers * 100L) / numberQuestionsPerSession, m, s);
        print(ANSI_BLUE, info);
    }

    private static void displayInfo(long end, int i) {
        long r = (end - System.currentTimeMillis()) / 1000;
        long m = (r % 3600) / 60;
        long s = r % 60;
        String info = String.format("Remaining questions: [%d] | Remaining Time: [%dm:%ds]", numberQuestionsPerSession - i, m, s);
        print(ANSI_BLUE, info);
    }

    private static void printQuestion(Question question) {
        print(ANSI_WHITE, "==========================================");
        String message = question.value;
        if (!message.contains("(") && !message.contains("True or False"))
            message += String.format(" [choose the best %d answers]", question.answers.size());

        print(ANSI_YELLOW, message);
        for (int i = 0; i < question.choices.size(); i++) {
            String choice = String.format("[%s] %s", i, question.choices.get(i));
            print("", choice);
        }
    }

    private static List<Question> parseFile() throws FileNotFoundException {
//        File file = new File("src/main/resources/qa/README.md");
        File file = new File("src/main/resources/qa/difficultQuestions.md");

        Scanner in = new Scanner(file);

        Question holder = null;
        List<Question> questions = new ArrayList<>();

        while (in.hasNext()) {
            String line = in.nextLine();
            if (line.startsWith("###")) {
                if (holder != null) questions.add(holder);
                holder = new Question(line.substring(4).trim());
            } else if (line.startsWith("-")) {
                String choice = line.substring(6);
                holder.choices.add(choice);
                if (line.startsWith("- [x] ")) holder.answers.add(choice);
            }
        }
        questions.add(holder);

        questions.forEach(question -> {
            if (!question.value.contains("True or False")) Collections.shuffle(question.choices);
        });
        return questions;
    }

    private static void displayWrongAnsweredQuestions() {
        print(ANSI_PURPLE, ">>>>>>>>>>>>>>>>>>>> *‿* <<<<<<<<<<<<<<<<<<<<");

        for (Question question : wrongAnsweredQuestions) {
            print(ANSI_YELLOW, question.value);
            for (String choice : question.choices) {
                if (question.answers.contains(choice)) {
                    print(ANSI_GREEN, "- [x] " + choice);
                } else {
                    print("", "- [ ] " + choice);
                }
            }
            print(ANSI_BLUE, "==========================================");
        }
        wrongAnsweredQuestions.clear();
    }

    private static void print(String color, String message) {
        System.out.println(color + message + ANSI_RESET);
    }

    static class Question {
        String value;
        List<String> choices = new ArrayList<>();
        List<String> answers = new ArrayList<>();
        private ArrayList<Object> userChoices;

        public Question(String value) {
            this.value = value;
        }

        public boolean isValidAnswer(char[] chars) {
            if (chars.length != answers.size()) return false;
            for (char c : chars) {
                int index = Character.getNumericValue(c);
                String choice = this.choices.get(index);
                if (!this.answers.contains(choice)) return false;
            }
            return true;
        }

        public void setUserChoices(char[] chars) {
            this.userChoices = new ArrayList<>();
            for (char c : chars) {
                int index = Character.getNumericValue(c);
                if (index < this.choices.size()) {
                    String choice = this.choices.get(index);
                    userChoices.add(choice);
                }
            }
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder("### " + value + '\n');
            for (String choice : choices) {
                s.append(answers.contains(choice) ? "- [x] " : "- [ ] ").append(choice).append('\n');
            }
            return s.toString();
        }
    }
}