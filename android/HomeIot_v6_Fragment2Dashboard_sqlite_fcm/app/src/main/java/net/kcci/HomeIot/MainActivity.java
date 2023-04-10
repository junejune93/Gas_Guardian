package net.kcci.HomeIot;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_CODE_LOGIN_ACTIVITY = 100;
    static Handler mainHandler;
    ClientThread clientThread;
    Fragment1Home fragment1Home;
    Database database;
    public SQLiteDatabase SQLdb;
    int bottomNavigationIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragment1Home = new Fragment1Home();

        database = new Database(this);
        SQLdb = database.getWritableDatabase();
        String dbName = database.getDatabaseName();
//        Log.d("MainActivity onCreate",dbName );
        if (dbName == null) {
            database.onCreate(SQLdb);
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment1Home).commit();


        mainHandler = new MainHandler();
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w("Main", "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        // Log and toast
                        String msg = getString(R.string.msg_token_fmt, token);
                        Log.d("Main", msg);
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    class MainHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            Bundle bundle = msg.getData();
            String data = bundle.getString("msg");
            if (data.indexOf("New con") != -1) {
                return;
            }
            if (bottomNavigationIndex == 0)
                fragment1Home.recvDataProcess(data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.login:
                Intent intent = new Intent(getBaseContext(), LoginActivity.class);
                intent.putExtra("serverIp", ClientThread.serverIp);
                intent.putExtra("serverPort", ClientThread.serverPort);
                intent.putExtra("clientId", ClientThread.clientId);
                intent.putExtra("clientPw", ClientThread.clientPw);
                startActivityForResult(intent, REQUEST_CODE_LOGIN_ACTIVITY);
                break;
            case R.id.db_delete:
                database.deleteTable(SQLdb);
                break;
            case R.id.setting:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_LOGIN_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                ClientThread.serverIp = data.getStringExtra("serverIp");
                ClientThread.serverPort = data.getIntExtra("serverPort", ClientThread.serverPort);
                ClientThread.clientId = data.getStringExtra("clientId");
                ClientThread.clientPw = data.getStringExtra("clientPw");
                clientThread = new ClientThread();
                clientThread.start();

            }
        }
    }
}
