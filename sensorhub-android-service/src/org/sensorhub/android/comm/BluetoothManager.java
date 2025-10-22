/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.android.comm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


public class BluetoothManager
{

    
    public List<BluetoothDevice> discoverDevices(Context context, final String macAddress)
    {
        final ArrayList<BluetoothDevice> foundDevices = new ArrayList<BluetoothDevice>();
        
        // find bluetooth SPP devices
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(new BroadcastReceiver()
        {
            public void onReceive(Context context, Intent intent)
            {
                String action = intent.getAction();
                
                if (BluetoothDevice.ACTION_FOUND.equals(action))
                {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    System.out.println("  Device: " + device.getAddress() + ", " + device);
                    
                    if (device.getAddress().matches(macAddress))
                        foundDevices.add(device);
                }
                else
                {
                    if (BluetoothDevice.ACTION_UUID.equals(action))
                    {
//                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                        Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
//                        for (int i = 0; i < uuidExtra.length; i++)
//                        {
//                            out.append("\n  Device: " + device.getName() + ", " + device + ", Service: " + uuidExtra[i].toString());
//                        }
                    }
                    else
                    {
                        if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
                        {
                            System.out.println("Discovery Started...");
                        }
                        else
                        {
                            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
                            {
                                System.out.println("\nDiscovery Finished");
//                                Iterator<bluetoothdevice> itr = btDeviceList.iterator();
//                                while (itr.hasNext())
//                                {
//                                    // Get Services for paired devices
//                                    BluetoothDevice device = itr.next();
//                                    out.append("\nGetting Services for " + device.getName() + ", " + device);
//                                    if (!device.fetchUuidsWithSdp())
//                                    {
//                                        out.append("\nSDP Failed for " + device.getName());
//                                    }
//
//                                }
                            }
                        }
                    }
                }
            }
        }, filter); // Don't forget to unregister during onDestroy

        // Getting the Bluetooth adapter
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        System.out.println("Adapter: " + btAdapter);
        btAdapter.startDiscovery();
        
        
        return foundDevices;
    }
    
    
    /**
     * Returns the first paired device whose name matches the given pattern
     * @param macAddress regular expression to match device names
     * @return first matching device
     * @throws IOException if a paired device with a matching name cannot be found
     */
    public BluetoothDevice findDevice(String macAddress) throws IOException
    {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

        if(btAdapter == null || !btAdapter.isEnabled()) throw new IOException("Bluetooth is not available or not enabled");

        for (BluetoothDevice dev: btAdapter.getBondedDevices())
        {
            if (dev.getAddress() != null && dev.getAddress().startsWith(macAddress)) {
                return dev;
            }
//            if(dev.getName() != null && dev.getName().startsWith(deviceNameRegex)){
//                return dev;
//            }
//            if (dev.getName().matches(deviceNameRegex))
//              return dev;
        }
        
        throw new IOException("Cannot find device " + macAddress);
    }
    
    
    public BluetoothSocket connectToSerialDevice(String macAddress) throws IOException
    {
        BluetoothDevice dev = findDevice(macAddress);
        UUID spp = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        BluetoothSocket socket = dev.createRfcommSocketToServiceRecord(spp);
        return socket;
    }
}
