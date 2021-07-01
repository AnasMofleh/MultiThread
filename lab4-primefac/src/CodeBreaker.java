import java.math.BigInteger;
import java.util.concurrent.*;
import javax.swing.*;
import client.view.ProgressItem;
import client.view.StatusWindow;
import client.view.WorklistItem;
import network.Sniffer;
import network.SnifferCallback;
import rsa.Factorizer;
import rsa.ProgressTracker;

public class CodeBreaker implements SnifferCallback {
    private final JProgressBar mainProgressBar;
    private final JPanel progressList;
    private final JPanel workList;


    private ExecutorService exec = Executors.newFixedThreadPool(2);

    // -----------------------------------------------------------------------

    private CodeBreaker() {
        StatusWindow w  = new StatusWindow();
        workList        = w.getWorkList();
        progressList    = w.getProgressList();
        mainProgressBar = w.getProgressBar();
//        w.enableErrorChecks();

        new Sniffer(this).start();
    }

    // -----------------------------------------------------------------------

    public static void main(String[] args) throws Exception {


        /*
         * Most Swing operations (such as creating view elements) must be
         * performed in the Swing EDT (Event Dispatch Thread).
         *
         * That's what SwingUtilities.invokeLater is for.
         */

        SwingUtilities.invokeLater(() -> new CodeBreaker());

    }

    // -----------------------------------------------------------------------

    /** Called by a Sniffer thread when an encrypted message is obtained. */
    @Override
    public void onMessageIntercepted(String message, BigInteger n) {
        SwingUtilities.invokeLater(() -> {

        var progressListItem = new ProgressItem(n, message);
        var workListItem = new WorklistItem(n, message);

        var buttonBreak = new JButton("Break");
        var buttonCancel = new JButton("Cancel");
        var buttonRemove = new JButton("remove");




        var progressTracker = new ProgressTracker() {
            @Override
            public void onProgress(int ppmDelta) {
                SwingUtilities.invokeLater(() ->{
                    progressListItem.getProgressBar().setMaximum(1000000);
                    progressListItem.getProgressBar().setValue(progressListItem.getProgressBar().getValue() + ppmDelta);
                    mainProgressBar.setValue(mainProgressBar.getValue() + ppmDelta);
                });
            }
        };

        workListItem.add(buttonBreak);
        workList.add(workListItem);

        buttonBreak.addActionListener(e -> {
            mainProgressBar.setMaximum(mainProgressBar.getMaximum() + 1000000);
            workList.remove(workListItem);
            progressListItem.add(buttonCancel);
            progressList.add(progressListItem);



            Future<String> future = exec.submit(() -> {
                String string = Factorizer.crack(message, n, progressTracker);
                progressListItem.getTextArea().setText(string);
                progressListItem.remove(buttonCancel);

                progressListItem.add(buttonRemove);
                buttonRemove.addActionListener(z -> {
                    var i = progressListItem.getProgressBar().getMaximum();
                    progressList.remove(progressListItem);
                    mainProgressBar.setMaximum(mainProgressBar.getMaximum() - 1000000);
                    mainProgressBar.setValue(mainProgressBar.getValue() - i);
                });
                return string;
            });

            buttonCancel.addActionListener(x -> {
                pop p = new pop(future);
            });

        });


    });
    }
}
