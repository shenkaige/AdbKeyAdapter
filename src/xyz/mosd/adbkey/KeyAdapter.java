package xyz.mosd.adbkey;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class KeyAdapter {

	public static void main(String args[]) {
		new KeyAdapter();
	}

	private static final String BTN_CMD_LOAD_DEVICES = "btn_cmd_Load_devices";
	private static final String HINT_GET_FOCUS = "Click here to start input key...";
	private static final String HINT_GOT_FOCUS_WAITING_INPUT = "key:";
	private JFrame jFrame;
	private JPanel rootPanel;
	private JScrollPane jScrollPaneCMD;
	private JTextArea jTextAreaCMD;
	private JComboBox<String> devicesList;
	private String deviceName;

	public KeyAdapter() {
		jFrame = new JFrame();
		jFrame.setTitle("AdbKeyAdapter");
		jFrame.setBounds(0, 0, 650, 300);
		rootPanel = new JPanel();
		jFrame.add(rootPanel);
		//
		BoxLayout layout = new BoxLayout(rootPanel, BoxLayout.X_AXIS);
		rootPanel.setLayout(layout);
		//
		JPanel header = new JPanel();
		header.setFocusable(false);
		devicesList = new JComboBox<String>();
		devicesList.addActionListener(mActionListener);
		header.add(devicesList);
		JButton btn = new JButton("Load Devices");
		btn.setActionCommand(BTN_CMD_LOAD_DEVICES);
		btn.addActionListener(mActionListener);
		header.add(btn);
		rootPanel.add(header);
		//
		jTextAreaCMD = new JTextArea();
		jTextAreaCMD.addKeyListener(mKeyListener);
		jTextAreaCMD.addFocusListener(mFocusListener);
		jTextAreaCMD.setText(HINT_GET_FOCUS);
		jScrollPaneCMD = new JScrollPane(jTextAreaCMD);
		jScrollPaneCMD.setBounds(0, 50, jFrame.getWidth(), 500);
		rootPanel.add(jScrollPaneCMD);
		//
		jFrame.setVisible(true);
		//
		loadDevicesList();
	}

	private final ActionListener mActionListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if (BTN_CMD_LOAD_DEVICES.equals(arg0.getActionCommand())) {
				loadDevicesList();
			} else if (arg0.getSource() == devicesList) {
				deviceName = (String) devicesList.getSelectedItem();
			}

		}
	};

	private final FocusListener mFocusListener = new FocusListener() {

		@Override
		public void focusLost(FocusEvent e) {
			jTextAreaCMD.setText(HINT_GET_FOCUS);
		}

		@Override
		public void focusGained(FocusEvent e) {
			jTextAreaCMD.setText(HINT_GOT_FOCUS_WAITING_INPUT);
		}
	};
	private final KeyListener mKeyListener = new KeyListener() {

		@Override
		public void keyTyped(KeyEvent e) {

		}

		@Override
		public void keyReleased(KeyEvent e) {

		}

		@Override
		public void keyPressed(KeyEvent e) {
			postKeyToAdb(e);
		}
	};

	private void appendCMDPrint(String msg) {
		String result = jTextAreaCMD.getText() + msg + "\n"
				+ HINT_GOT_FOCUS_WAITING_INPUT;
		jTextAreaCMD.setText(result);
	}

	// -----------------------------------------------
	private boolean haveRefreshDeviceTaskRunning = false;

	private synchronized void loadDevicesList() {
		if (haveRefreshDeviceTaskRunning) {
			return;
		}
		new Thread() {
			@Override
			public void run() {
				try {
					Process p = Runtime.getRuntime().exec("adb devices");
					InputStream is = p.getInputStream();
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(is));
					String line = null;
					List<String> result = new ArrayList<String>();
					while ((line = reader.readLine()) != null) {
						result.add(line.replace("device", "").trim());
					}
					p.destroy();
					if (!result.isEmpty()) {
						result.remove(0);
					}
					DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>();
					if (!result.isEmpty()) {
						for (String s : result) {
							model.addElement(s);
						}
					}
					devicesList.setModel(model);
				} catch (IOException e) {
					e.printStackTrace();
				}
				synchronized (KeyAdapter.this) {
					haveRefreshDeviceTaskRunning = false;
				}
			}

		}.start();

	}

	private void postKeyToAdb(KeyEvent e) {
		Integer code = keyMap.get(e.getKeyCode());
		if (code == null) {
			appendCMDPrint("\n unsupport key:" + e.getKeyChar());
		} else {
			new PostKeyEventThread(code).start();
			appendCMDPrint("PC:" + KeyEvent.getKeyText(e.getKeyCode())
					+ "->android:" + String.valueOf(code));
		}
	}

	class PostKeyEventThread extends Thread {
		private final int keycode;

		public PostKeyEventThread(int keycode) {
			this.keycode = keycode;
		}

		@Override
		public void run() {
			try {
				StringBuilder sb = new StringBuilder();
				sb.append("adb");
				if (deviceName != null) {
					sb.append(" -s ");
					sb.append(deviceName);
				}
				sb.append(" shell input keyevent ");
				sb.append(keycode);
				final String cmd = sb.toString();
				Runtime.getRuntime().exec(cmd);
				System.out.println(cmd);
			} catch (IOException e) {
				e.printStackTrace();
				appendCMDPrint(e.toString());
			}
		}
	}

	private final static int ANDROID_KEYCODE_ENTER = 66;// ENTER
	private final static int ANDROID_KEYCODE_BACK = 4;// Back
	// private final static int ANDROID_KEYCODE_ESCAPE = 111;// ESC
	// private final static int ANDROID_KEYCODE_DPAD_CENTER = 23;
	private final static int ANDROID_KEYCODE_DPAD_UP = 19;
	private final static int ANDROID_KEYCODE_DPAD_DOWN = 20;
	private final static int ANDROID_KEYCODE_DPAD_LEFT = 21;
	private final static int ANDROID_KEYCODE_DPAD_RIGHT = 22;
	private final static int ANDROID_KEYCODE_MOVE_HOME = 122;
	private final static int ANDROID_KEYCODE_MOVE_END = 123;
	private final static int ANDROID_KEYCODE_PAGE_UP = 92;
	private final static int ANDROID_KEYCODE_PAGE_DOWN = 93;
	/** Key code constant: '0' key. */
	private final static int ANDROID_KEYCODE_0 = 7;
	/** Key code constant: '1' key. */
	private final static int ANDROID_KEYCODE_1 = 8;
	/** Key code constant: '2' key. */
	private final static int ANDROID_KEYCODE_2 = 9;
	/** Key code constant: '3' key. */
	private final static int ANDROID_KEYCODE_3 = 10;
	/** Key code constant: '4' key. */
	private final static int ANDROID_KEYCODE_4 = 11;
	/** Key code constant: '5' key. */
	private final static int ANDROID_KEYCODE_5 = 12;
	/** Key code constant: '6' key. */
	private final static int ANDROID_KEYCODE_6 = 13;
	/** Key code constant: '7' key. */
	private final static int ANDROID_KEYCODE_7 = 14;
	/** Key code constant: '8' key. */
	private final static int ANDROID_KEYCODE_8 = 15;
	/** Key code constant: '9' key. */
	private final static int ANDROID_KEYCODE_9 = 16;
	/** Key code constant: 'A' key. */
	private final static int ANDROID_KEYCODE_A = 29;
	/** Key code constant: 'B' key. */
	private final static int ANDROID_KEYCODE_B = 30;
	/** Key code constant: 'C' key. */
	private final static int ANDROID_KEYCODE_C = 31;
	/** Key code constant: 'D' key. */
	private final static int ANDROID_KEYCODE_D = 32;
	/** Key code constant: 'E' key. */
	private final static int ANDROID_KEYCODE_E = 33;
	/** Key code constant: 'F' key. */
	private final static int ANDROID_KEYCODE_F = 34;
	/** Key code constant: 'G' key. */
	private final static int ANDROID_KEYCODE_G = 35;
	/** Key code constant: 'H' key. */
	private final static int ANDROID_KEYCODE_H = 36;
	/** Key code constant: 'I' key. */
	private final static int ANDROID_KEYCODE_I = 37;
	/** Key code constant: 'J' key. */
	private final static int ANDROID_KEYCODE_J = 38;
	/** Key code constant: 'K' key. */
	private final static int ANDROID_KEYCODE_K = 39;
	/** Key code constant: 'L' key. */
	private final static int ANDROID_KEYCODE_L = 40;
	/** Key code constant: 'M' key. */
	private final static int ANDROID_KEYCODE_M = 41;
	/** Key code constant: 'N' key. */
	private final static int ANDROID_KEYCODE_N = 42;
	/** Key code constant: 'O' key. */
	private final static int ANDROID_KEYCODE_O = 43;
	/** Key code constant: 'P' key. */
	private final static int ANDROID_KEYCODE_P = 44;
	/** Key code constant: 'Q' key. */
	private final static int ANDROID_KEYCODE_Q = 45;
	/** Key code constant: 'R' key. */
	private final static int ANDROID_KEYCODE_R = 46;
	/** Key code constant: 'S' key. */
	private final static int ANDROID_KEYCODE_S = 47;
	/** Key code constant: 'T' key. */
	private final static int ANDROID_KEYCODE_T = 48;
	/** Key code constant: 'U' key. */
	private final static int ANDROID_KEYCODE_U = 49;
	/** Key code constant: 'V' key. */
	private final static int ANDROID_KEYCODE_V = 50;
	/** Key code constant: 'W' key. */
	private final static int ANDROID_KEYCODE_W = 51;
	/** Key code constant: 'X' key. */
	private final static int ANDROID_KEYCODE_X = 52;
	/** Key code constant: 'Y' key. */
	private final static int ANDROID_KEYCODE_Y = 53;
	/** Key code constant: 'Z' key. */
	private final static int ANDROID_KEYCODE_Z = 54;
	public final static int ANDROID_KEYCODE_DEL = 67;

	private final static Map<Integer, Integer> keyMap = new HashMap<Integer, Integer>();
	static {
		keyMap.put(KeyEvent.VK_ENTER, ANDROID_KEYCODE_ENTER);
		keyMap.put(KeyEvent.VK_ESCAPE, ANDROID_KEYCODE_BACK);
		keyMap.put(KeyEvent.VK_UP, ANDROID_KEYCODE_DPAD_UP);
		keyMap.put(KeyEvent.VK_DOWN, ANDROID_KEYCODE_DPAD_DOWN);
		keyMap.put(KeyEvent.VK_LEFT, ANDROID_KEYCODE_DPAD_LEFT);
		keyMap.put(KeyEvent.VK_RIGHT, ANDROID_KEYCODE_DPAD_RIGHT);
		keyMap.put(KeyEvent.VK_HOME, ANDROID_KEYCODE_MOVE_HOME);
		keyMap.put(KeyEvent.VK_END, ANDROID_KEYCODE_MOVE_END);
		keyMap.put(KeyEvent.VK_PAGE_UP, ANDROID_KEYCODE_PAGE_UP);
		keyMap.put(KeyEvent.VK_PAGE_DOWN, ANDROID_KEYCODE_PAGE_DOWN);
		//
		keyMap.put(KeyEvent.VK_0, ANDROID_KEYCODE_0);
		keyMap.put(KeyEvent.VK_1, ANDROID_KEYCODE_1);
		keyMap.put(KeyEvent.VK_2, ANDROID_KEYCODE_2);
		keyMap.put(KeyEvent.VK_3, ANDROID_KEYCODE_3);
		keyMap.put(KeyEvent.VK_4, ANDROID_KEYCODE_4);
		keyMap.put(KeyEvent.VK_5, ANDROID_KEYCODE_5);
		keyMap.put(KeyEvent.VK_6, ANDROID_KEYCODE_6);
		keyMap.put(KeyEvent.VK_7, ANDROID_KEYCODE_7);
		keyMap.put(KeyEvent.VK_8, ANDROID_KEYCODE_8);
		keyMap.put(KeyEvent.VK_9, ANDROID_KEYCODE_9);
		keyMap.put(KeyEvent.VK_A, ANDROID_KEYCODE_A);
		keyMap.put(KeyEvent.VK_B, ANDROID_KEYCODE_B);
		keyMap.put(KeyEvent.VK_C, ANDROID_KEYCODE_C);
		keyMap.put(KeyEvent.VK_D, ANDROID_KEYCODE_D);
		keyMap.put(KeyEvent.VK_E, ANDROID_KEYCODE_E);
		keyMap.put(KeyEvent.VK_F, ANDROID_KEYCODE_F);
		keyMap.put(KeyEvent.VK_G, ANDROID_KEYCODE_G);
		keyMap.put(KeyEvent.VK_H, ANDROID_KEYCODE_H);
		keyMap.put(KeyEvent.VK_I, ANDROID_KEYCODE_I);
		keyMap.put(KeyEvent.VK_J, ANDROID_KEYCODE_J);
		keyMap.put(KeyEvent.VK_K, ANDROID_KEYCODE_K);
		keyMap.put(KeyEvent.VK_L, ANDROID_KEYCODE_L);
		keyMap.put(KeyEvent.VK_M, ANDROID_KEYCODE_M);
		keyMap.put(KeyEvent.VK_N, ANDROID_KEYCODE_N);
		keyMap.put(KeyEvent.VK_O, ANDROID_KEYCODE_O);
		keyMap.put(KeyEvent.VK_P, ANDROID_KEYCODE_P);
		keyMap.put(KeyEvent.VK_Q, ANDROID_KEYCODE_Q);
		keyMap.put(KeyEvent.VK_R, ANDROID_KEYCODE_R);
		keyMap.put(KeyEvent.VK_S, ANDROID_KEYCODE_S);
		keyMap.put(KeyEvent.VK_T, ANDROID_KEYCODE_T);
		keyMap.put(KeyEvent.VK_U, ANDROID_KEYCODE_U);
		keyMap.put(KeyEvent.VK_V, ANDROID_KEYCODE_V);
		keyMap.put(KeyEvent.VK_W, ANDROID_KEYCODE_W);
		keyMap.put(KeyEvent.VK_X, ANDROID_KEYCODE_X);
		keyMap.put(KeyEvent.VK_Y, ANDROID_KEYCODE_Y);
		keyMap.put(KeyEvent.VK_Z, ANDROID_KEYCODE_Z);
		keyMap.put(KeyEvent.VK_BACK_SPACE, ANDROID_KEYCODE_DEL);

	}

}
