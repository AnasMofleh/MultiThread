// Java Program to create a popup and display 
// it on a parent frame 
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Future;

class pop extends JFrame implements ActionListener {
    // popup 
    Popup p;
    JFrame f;
    private Future<String> future;

    // constructor 
    pop(Future future)
    {
        this.future = future;
        // create a frame 
         f = new JFrame("Are u sure you want to terminate?");



        f.setSize(400, 70);

        PopupFactory pf = new PopupFactory();

        // create a panel 
        JPanel p1 = new JPanel();



        // create a popup
        p = pf.getPopup(f, p1, 50, 50);

        // create a button
        JButton buttonYes = new JButton("Yes");
        JButton buttonNo = new JButton("No");

        // add action listener
        buttonYes.addActionListener(this );
        buttonNo.addActionListener(x -> {
          f.hide();
        });

        p1.add(buttonYes);
        p1.add(buttonNo);
        f.add(p1);
        f.show();

    }



    @Override
    public void actionPerformed(ActionEvent e) {
        future.cancel(true);
        f.hide();
    }
}