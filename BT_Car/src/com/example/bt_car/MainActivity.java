package com.example.bt_car;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.Manifest;
import android.R.bool;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements OnTouchListener,OnCheckedChangeListener{
	//��
	public final int CONNECT_FAILED = 0;
	public final int CONNECT_SUCCESS = 1;
	public final int READ_FAILED = 2;
	public final int WRITE_FAILED = 3;
	public final int DATA = 4;
	static final int CMD_SEND_DATA    = 0x04;
	private String bluetoothAddr = "20:16:04:18:55:23";/*�����豸��ַ*/
	//���
	Button sensorMode;
	ToggleButton remoteMode;
	MySurfaceView mySurfaceView;
	//ȫ�ֱ���
	public boolean isConnecting,pairing=false;
	private BluetoothAdapter bluetoothAdapter;
	BroadcastReceiver mReceiver;
	BluetoothSocket socket=null;
	DirSensor sensor;
    //���·���
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//�����ʼ��
		widgetInit();
		//������Ӧ��ʼ��
		sensor = new DirSensor(MainActivity.this);
		//��ȡ����������
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		//�ж��з������
		if(!bluetoothAdapter.isEnabled()){
			//������ʾ��ǿ�д�
			bluetoothAdapter.enable();
		}		
		//��׿6.0��ȡɨ��Ȩ��
		getScanPermission();
		//ע��㲥
		registerBTBroadcast();
		//����ɨ��
		scanOrNot(true);
		showToast("��ʼɨ��С��...");	
	}
	private void widgetInit() {
		//���ʵ����
		mySurfaceView = (MySurfaceView) findViewById(R.id.gameView);
		sensorMode = (Button) findViewById(R.id.btn_sensor);
		remoteMode = (ToggleButton) findViewById(R.id.remoteMode);
		//�������
		sensorMode.setOnTouchListener(this);
		//δ����ǰ����ң��ģʽ��ť
		remoteMode.setOnCheckedChangeListener(this);
		remoteMode.setClickable(false);
	}
	private void getScanPermission() {
		//�ж��Ƿ���Ȩ��
		if (ContextCompat.checkSelfPermission(this,
		        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			//����Ȩ��
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},1);
			//�ж��Ƿ���Ҫ ���û����ͣ�ΪʲôҪ�����Ȩ��
			if(ActivityCompat.shouldShowRequestPermissionRationale(this,
			        Manifest.permission.READ_CONTACTS)) {
			    Toast.makeText(this, "shouldShowRequestPermissionRationale", Toast.LENGTH_SHORT).show();
			}
		}
	}
	public void registerBTBroadcast() {
		mReceiver = new BroadcastReceiver() {  		    
			public void onReceive(Context context, Intent intent) {  
		        String action = intent.getAction();  
		        //�ҵ��豸  
		        if (BluetoothDevice.ACTION_FOUND.equals(action)) {  
		            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);  
		            Log.e("BT_scan", "ɨ�赽�����豸:(" + device.getName()  
		                        +"->"+ device.getAddress()+")");
		            //����豸��С��������
		            if (device.getAddress().equals(bluetoothAddr)) {
		            	Log.e("BT_scan", "ɨ�赽С��");
		            	// ���������豸�Ĺ���ռ����Դ�Ƚ϶ֹ࣬ͣɨ��
		            	scanOrNot(false);
		            	// ��ȡ�����豸�����״̬		     
		            	switch (device.getBondState()) {
			            	// δ���
			            	case BluetoothDevice.BOND_NONE:
			            		Log.e("BT_bond", "������δ��ԣ���Կ�ʼ��");
				            	try {
					            	Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
					            	boolean pairReturn = (Boolean) createBondMethod.invoke(device);
					            	pairing = true;
				            	} catch (Exception e) {
				            		e.printStackTrace();
				            	}
				            	break;
			            	// �����
			            	case BluetoothDevice.BOND_BONDED:
			            		Log.e("BT_bond", "�����Ѿ���ԣ����ӿ�ʼ��");
				            	try {
					            	// ����
					            	connect(device);
				            	} catch (IOException e) {
				            		e.printStackTrace();
				            	}
				            	break;
				            // �����
			            	case BluetoothDevice.BOND_BONDING:
			            		Log.e("BT_bond", "��������ԵĹ���(Pairing)��");
			            		break;
		            	}
					}
		        }  
		        //�������  
		        else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED  
		                .equals(action)) {  
		            if (!isConnecting) {
		            	scanOrNot(true);
		            }
		        }
		        //�ɹ�����
		        else if (BluetoothDevice.ACTION_ACL_CONNECTED  
		                .equals(action)) {  
		        	connectedHandle();
		        } 
		        //���ӶϿ�
		        else if (BluetoothDevice.ACTION_ACL_DISCONNECTED  
		                .equals(action)) {  
		        	disconnectedHandle();		        	
		        } 
		        //��״̬�ı�
		        else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED  
		                .equals(action)) {  
		        	int cur_bond_state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
		        	String state = null;
		        	if (cur_bond_state==BluetoothDevice.BOND_BONDED) {
		        		state = "��Գɹ���";
					}else if (cur_bond_state==BluetoothDevice.BOND_BONDING){
						state = "�������...";
					}else if (cur_bond_state==BluetoothDevice.BOND_NONE){
						state = "�����Ч";
					}
		        	Toast.makeText(MainActivity.this,state, Toast.LENGTH_SHORT)
					.show();		
		        }
		        /*ң��״̬�ı�*/
		        else if (action.equals("com.example.bt_car.mysurfaceview") ){
					int cmd = intent.getIntExtra("cmd", -1);
					int value = intent.getIntExtra("value", -1);
					if (cmd==CMD_SEND_DATA&&isConnecting) {
						sendMessage(String.valueOf(value));
					}
				}
		        /*������Ӧ״̬�ı�*/
		        else if (action.equals("com.example.bt_car.dirsensor") ){
		        	//Log.e("BT_recvSensor", "!!!!!!");
					int cmd = intent.getIntExtra("cmd", -1);
					float x = intent.getFloatExtra("x", -1);
					float y = intent.getFloatExtra("y", -1);
					mySurfaceView.setRockPosition(x*60, y*60);
					//��ֹ�ظ�����
					if (cmd==1&&isConnecting) {					
						int logicStatus = intent.getIntExtra("logicStatus", -1);
						sendMessage(String.valueOf(logicStatus));
					}
				}
		    }  
		};  

		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND); 
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		filter.addAction("com.example.bt_car.mysurfaceview");
		filter.addAction("com.example.bt_car.dirsensor");
		registerReceiver(mReceiver, filter);  
	}
	private void connect(BluetoothDevice device) throws IOException {
		// �̶���UUID
		final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
		UUID uuid = UUID.fromString(SPP_UUID);
		socket = device.createRfcommSocketToServiceRecord(uuid);
		socket.connect();
	}
	// ���巢�ͺ���
	public void sendMessage(String message) {
		OutputStream outputstream = null;
		if (socket==null||isConnecting==false) {
			Toast.makeText(MainActivity.this, "����δ����", Toast.LENGTH_SHORT)
			.show();
			return;
		}
		try {
			outputstream = socket.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(MainActivity.this, "�ͻ������������ʧ��", Toast.LENGTH_SHORT)
					.show();
		}
		try {
			outputstream.write(message.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(MainActivity.this, "��������ʧ��", Toast.LENGTH_SHORT)
					.show();
		}
	}
	//״̬��Ϊ������
	public void connectedHandle() {
		Log.d("BT_connect", "�������ӳɹ���"); 
		showToast("���ӳɹ���");
		//ֹͣɨ��
    	scanOrNot(false);
		isConnecting = true;
		remoteMode.setClickable(true);
		setTitle("С��������"); 
	}
	//״̬��Ϊ���ӶϿ�
	public void disconnectedHandle() {
		showToast("С�����ӶϿ�");
		Log.d("BT_connect", "�������ӶϿ���"); 
		isConnecting = false;
		remoteMode.setChecked(false);
		remoteMode.setClickable(false);
		setTitle("С�����ӶϿ�,����ɨ��"); 
		//����ɨ��
		scanOrNot(true);
		
	}
	public void scanOrNot(boolean trueOrNot) {
        if (trueOrNot) {
        	//��ȡ����������
    		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    		//�ж��з������
    		if(!bluetoothAdapter.isEnabled()){
    			//������ʾ��ǿ�д�
    			bluetoothAdapter.enable();
    		}
    		//����ɨ��
    		bluetoothAdapter.startDiscovery();
    		setTitle("����ɨ��");
		}else if(bluetoothAdapter.isDiscovering()){
			bluetoothAdapter.cancelDiscovery();
			setTitle("ɨ��ֹͣ");
		}
    }
	public void showToast(String showString) {
		Toast.makeText(MainActivity.this, showString, Toast.LENGTH_SHORT).show();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		//��ȡ����������
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		//�ж��з������
		if(!bluetoothAdapter.isEnabled()){
			//������ʾ��ǿ�д�
			bluetoothAdapter.enable();
		}
	}
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.btn_sensor:
			if(event.getAction() == MotionEvent.ACTION_DOWN/*&&isConnecting*/){   
				//sendMessage("1");
				sensor.sensorListenerStart();
			}
			else if(event.getAction()==MotionEvent.ACTION_UP/*&&isConnecting*/){
				//sendMessage("0");
				sensor.sensorListenerStop();
			} 
			break;	
		}		
		return true;
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		// TODO Auto-generated method stub
		if (buttonView.getId()==R.id.remoteMode) {
			if (isChecked) {
				if (isConnecting) {
					//����ң��ģʽ
					sendMessage("9"); 
					Log.e("BT_remote", "����ң��ģʽ");
				}				
			}else {
				if (isConnecting) {
					//�˳�ң��ģʽ
					sendMessage("a");
					Log.e("BT_remote", "�˳�ң��ģʽ");
				}			
			}			
		}
	}
	//�ٰ�һ���˳�����
	private long exitTime = 0;
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			if ((System.currentTimeMillis() - exitTime) > 2000) {
				Toast.makeText(getApplicationContext(), "�ٰ�һ���˳�������",
						Toast.LENGTH_SHORT).show();
				exitTime = System.currentTimeMillis();
			} else {			
				finish();
				System.exit(0);
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
	
