package com.dataart.btle_android.devicehive.btledh;

import android.os.Build;

import com.dataart.btle_android.devicehive.BTLEDevicePreferences;
import com.dataart.btle_android.devicehive.DeviceHiveConfig;
import com.github.devicehive.client.model.CommandFilter;
import com.github.devicehive.client.model.DHResponse;
import com.github.devicehive.client.model.DeviceCommandsCallback;
import com.github.devicehive.client.model.FailureData;
import com.github.devicehive.client.service.Device;
import com.github.devicehive.client.service.DeviceCommand;
import com.github.devicehive.client.service.DeviceHive;

import java.util.List;

public class BTLEDeviceHive {

    private DeviceHive deviceHive;
    private BTLEDevicePreferences prefs;

    private CommandListener commandListener;

    public BTLEDeviceHive() {
        prefs = BTLEDevicePreferences.getInstance();
        deviceHive = DeviceHive.getInstance().init(prefs.getServerUrl(), prefs.getRefreshToken());
    }

    public DeviceHive getDeviceHive() {
        if (deviceHive == null) {
            deviceHive.init(prefs.getServerUrl(), prefs.getRefreshToken());
        }
        return this.deviceHive;
    }

    public static String getDeviceName() {
        final String manufacturer = Build.MANUFACTURER;
        final String model = Build.MODEL;
        return model.startsWith(manufacturer) ? model : manufacturer + " " + model;
    }

    public void setCommandListener(CommandListener listener) {
        this.commandListener = listener;
    }

    public void removeCommandListener() {
        commandListener = null;
    }

    private void notifyListenersCommandReceived(DeviceCommand command) {
        commandListener.onDeviceReceivedCommand(command);
    }

    public static BTLEDeviceHive newInstance() {
        BTLEDevicePreferences prefs = BTLEDevicePreferences.getInstance();

        BTLEDeviceHive device = new BTLEDeviceHive();

        device.getDeviceHive().enableDebug(true);

        String serverUrl = prefs.getServerUrl();

        if (serverUrl == null) {
            serverUrl = DeviceHiveConfig.API_ENDPOINT;
            prefs.setServerUrlSync(serverUrl);
        }
        return device;
    }

    public void registerDevice() {
        Thread thread = new Thread(new Runnable(){
            public void run() {
                try {
                    DHResponse<Device> devicehiveResponse = deviceHive.getDevice(prefs.getGatewayId());
                    Device device = devicehiveResponse.getData();
                    device.subscribeCommands(new CommandFilter(), new DeviceCommandsCallback() {
                        public void onSuccess(List<DeviceCommand> commands) {
                            for(DeviceCommand command: commands) {
                                notifyListenersCommandReceived(command);
                            }
                        }
                        public void onFail(FailureData failureData) {
                        }
                });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

}