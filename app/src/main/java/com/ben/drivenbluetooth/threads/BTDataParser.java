package com.ben.drivenbluetooth.threads;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.ben.drivenbluetooth.Global;
import com.ben.drivenbluetooth.events.ArduinoEvent;
import com.ben.drivenbluetooth.util.GearHelper;
//import com.ben.drivenbluetooth.util.GraphData;
import com.ben.drivenbluetooth.util.RunningAverage;

import org.greenrobot.eventbus.EventBus;

public class BTDataParser extends Thread {
    public static Handler mHandler;
    private byte[] poppedData;

    /* == Amp Hour Variables ==*/
    private double prevAmps = 0.0;
    private long prevAmpTime = 0;

    /* == Watt hour variables ==*/
    private final RunningAverage WattHourAvg = new RunningAverage(0); // decimal format doesn't matter because we don't use the print function

    /* == Distance variables ==*/
    private long prevDistTime = 0;

    /*===================*/
    /* MAIN FUNCS
	/*===================*/
    @Override
    public void run() {
        Looper.prepare();

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                poppedData = Global.BTStreamQueue.poll();
                if (poppedData != null) {
                /* poppedData should look like {sXY}
                 * { } are the packet container identifiers
                 * s identifies what kind of information it is
                 */

                    double value; // return value

                    // First check if the packet is valid
                    if (poppedData.length == Global.PACKETLENGTH    // check if correct length
                            && poppedData[0] == Global.STARTBYTE    // check if starts with '{'
                            && poppedData[Global.PACKETLENGTH - 1] == Global.STOPBYTE) { // check if ends with '}'

                        /* First send the packet over UDP */
//                        if (MainActivity.mTelemetrySender != null) {
//                            Message packet = Message.obtain();
//                            packet.obj = poppedData;
//                            //MainActivity.mTelemetrySender.PacketHandler.sendMessage(packet);
//                        }

                        byte firstByte = poppedData[2];
                        byte secondByte = poppedData[3];

                        // data is good
                        // Now for the hard part
                        // if the byte is 255 / 0xFF / 11111111 then the value is interpreted as zero
                        // because you can't send null bytes over Bluetooth.
                        // A side-effect of this is that we can't send the value "255"
                        // a byte in Java is -128 to 127 so we must convert to an int by doing & 0xff

						/* Explanation :
                         * We have defined 255 (0xFF) to be zero because we can't send null bytes over Bluetooth
						 * & is a bitwise AND operation
						 * (byte) 1111111 is interpreted by Java as -128
						 * 11111111 & 0xff converts the value to an integer, which is then interpreted as
						 * (int) 255, thus enabling the proper comparison
						 */

                        if ((firstByte & 0xff) == 255) {
                            firstByte = 0;
                        }
                        if ((secondByte & 0xff) == 255) {
                            secondByte = 0;
                        }

						/* if the first byte is greater than 127 then the value is treated as an INTEGER
						 * value = [first byte] * 100 + [second byte]
						 *
						 * if the first byte is less than 128 then the value is treated as a FLOAT
						 * value = [first byte] + [second byte] / 100
						 */

                        if ((firstByte & 0xff) < 128) {
                            // FLOAT
                            // value = [first byte] + [second byte] / 100
                            value = (double) (firstByte & 0xff) + (double) (secondByte & 0xff) / 100;
                        } else {
                            // INTEGER
                            // value = [first byte] * 100 + [second byte]
                            firstByte -= 128;
                            value = (double) (firstByte & 0xff) * 100 + (double) (secondByte & 0xff);
                        }
                        //TODO - Stop BT data coming through in Test Mode

                        // Check the ID
                        switch (poppedData[1]) {
                            case Global.VOLTS_ID:
                                SetVolts(value);
                                break;
                            case Global.VOLTS_AUX_ID:
                                SetVoltsAux(value);
                                break;
                            case Global.AMPS_ID:
                                SetAmps(value);
                                break;
                            case Global.MOTOR_RPM_ID:
                                SetMotorRPM(value);
                                break;
                            case Global.SPEED_MPS_ID:
                                SetSpeed(value);
                                break;
                            case Global.THR_INPUT_ID:
                                SetInputThrottle(value);
                                break;
                            case Global.THR_ACTUAL_ID:
                                SetActualThrottle(value);
                                break;
                            case Global.TEMP1ID:
                                SetTemperature(value, 1);
                                break;
                            case Global.TEMP2ID:
                                SetTemperature(value, 2);
                                break;
                            case Global.TEMP3ID:
                                SetTemperature(value, 3);
                                break;
                            case Global.GEAR_RATIO_ID:
                                SetGearRatio(value);
                                break;
                            case Global.LAUNCH_MODE_ID:
                                EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.LaunchMode, 0));
                                break;
                            case Global.CYCLE_VIEW_ID:
                                EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.CycleView, 0));
                                break;
                            case Global.LOOP_COUNTER_ID:
                                SetPerformanceMetric(value);
                                break;
                            case Global.BRAKE_ID:
                                SetBrake(value);
                                break;
//                            case Global.FAN_DUTY_ID:
//                                SetFanDuty(value);
//                                break;
                            case Global.STEERING_ID:
                                SetSteeringAngle(value);
                                break;
                            case Global.CUSTOM_0:
                                SetCustomData(value, 0);
                                break;
                            case Global.CUSTOM_1:
                                SetCustomData(value, 1);
                                break;
                            case Global.CUSTOM_2:
                                SetCustomData(value, 2);
                                break;
                            case Global.CUSTOM_3:
                                SetCustomData(value, 3);
                                break;
                            case Global.CUSTOM_4:
                                SetCustomData(value, 4);
                                break;
                            case Global.CUSTOM_5:
                                SetCustomData(value, 5);
                                break;
                            case Global.CUSTOM_6:
                                SetCustomData(value, 6);
                                break;
                            case Global.CUSTOM_7:
                                SetCustomData(value, 7);
                                break;
                            case Global.CUSTOM_8:
                                SetCustomData(value, 8);
                                break;
                            case Global.CUSTOM_9:
                                SetCustomData(value, 9);
                                break;
                            default:
                                Global.MangledDataCount++;
                                return false;
                        }

                        return true;
                    } else {
                        // data is bad
                        Global.MangledDataCount++;
                        return false;
                    }
                }
                return false;
            }
        });
        Looper.loop();
    }

    /*===================*/
	/* DATA INPUT FUNCS
	/*===================*/
    private synchronized void SetVolts(final double rawVolts) {
        Global.Volts = rawVolts; // Apply conversion and offset
        if (Global.Lap > 0) {
            Global.LapDataList.get(Global.Lap - 1).AddVolts(rawVolts);
        }
//        GraphData.AddToHistory(rawVolts, Global.VoltsHistory);
        EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.Volts, rawVolts));
    }

    private synchronized void SetVoltsAux(final double rawVolts) {
        Global.VoltsAux = rawVolts; // Apply conversion and offset
        EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.VoltsAux, rawVolts));
    }

    private synchronized void SetAmps(final double rawAmps) {
        Global.Amps = rawAmps;
        // timekeeping for watt-hours and amp-hours
        long millis = System.currentTimeMillis();
        long dt = millis - prevAmpTime;
        if (prevAmpTime > 0) {
            IncrementAmpHours(Global.Amps, dt);
            CalculateWattHours(Global.Volts, Global.Amps, dt);
        }
        prevAmps = rawAmps;
        prevAmpTime = millis;

        Global.AverageAmps.add(rawAmps);
        if (Global.Lap > 0) {
            Global.LapDataList.get(Global.Lap - 1).AddAmps(rawAmps);
        }
//        GraphData.AddToHistory(rawAmps, Global.AmpsHistory);

        EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.Amps, rawAmps));
    }

    private synchronized void SetInputThrottle(final double rawThrottle) {
        Global.InputThrottle = rawThrottle; // Apply conversion and offset
//        GraphData.AddToHistory(rawThrottle, Global.ThrottleHistory);

        EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.ThrottleInput, rawThrottle));
    }

    private synchronized void SetActualThrottle(double rawThrottle) {
        Global.ActualThrottle = rawThrottle; // Apply conversion and offset
        EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.ThrottleActual, rawThrottle));
    }

    private synchronized void SetSpeed(final double rawSpeedMPS) {
        Global.SpeedMPS = rawSpeedMPS;

        long millis = System.currentTimeMillis();
        long dt = millis - prevDistTime;
        if (prevDistTime > 0) {
            CalculateDistanceMeters(Global.SpeedMPS, dt);
        }
        prevDistTime = millis;

        Global.AverageSpeedMPS.add(Global.SpeedMPS);

        if (Global.Lap > 0) {
            Global.LapDataList.get(Global.Lap - 1).AddSpeedMPS(Global.SpeedMPS);
        }

//        GraphData.AddToHistory(Global.SpeedUnit == Global.UNIT.KPH ? Global.SpeedMPS * 3.6 : Global.SpeedMPS * 2.2, Global.SpeedHistory);

        EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.WheelSpeedMPS, rawSpeedMPS));
    }

    private synchronized void SetMotorRPM(final double rawMotorRPM) {
        Global.MotorRPM = rawMotorRPM; // Apply conversion and offset
        if (Global.Lap > 0) {
            Global.LapDataList.get(Global.Lap - 1).AddRPM(rawMotorRPM);
        }
//        GraphData.AddToHistory(rawMotorRPM, Global.MotorRPMHistory);

        EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.MotorSpeedRPM, rawMotorRPM));
    }

    private synchronized void SetTemperature(double rawTemp, final int sensorId) {
        switch (sensorId) {
            case 1:
                Global.TempC1 = rawTemp;
//                GraphData.AddToHistory(rawTemp, Global.TempC1History);
                EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.TemperatureA, rawTemp));
                break;
            case 2:
                Global.TempC2 = rawTemp;
                EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.TemperatureB, rawTemp));
                break;
            case 3:
                Global.TempC3 = rawTemp;
                EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.TemperatureC, rawTemp));
                break;
            default:
                break;
        }
    }

    private synchronized void SetGearRatio(double rawRatio) {
        Global.GearRatio = rawRatio; // Apply conversion and offset
        Global.Gear = GearHelper.GetGear(rawRatio, Global.MotorTeeth, Global.WheelTeeth);
        EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.GearRatio, rawRatio));
    }

    private synchronized void SetPerformanceMetric(double pm) {
        Global.PerformanceMetric = pm;
        EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.PerformanceMetric, pm));
    }

    private synchronized void SetBrake(final double brake) {
        Global.Brake = (int) brake;
        EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.BrakeStatus, brake));
    }

    private synchronized void SetFanDuty(final double fan) {
        Global.FanDuty = fan;
        EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.FanDuty, fan));
    }

    private synchronized void SetSteeringAngle(final double angle) {
        Global.SteeringAngle = angle;
    }

    private synchronized void IncrementAmpHours(double amps, long dt_millis) {
        double ah = 0.5 * (amps + prevAmps) * dt_millis /* Amp-milliseconds */
                / 1000 / 60 / 60; /* Amp-hours */

        Global.AmpHours += ah;

        if (Global.Lap > 0) {
            Global.LapDataList.get(Global.Lap - 1).AddAmpHours(ah);
        }

        EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.AmpHours, ah));
    }

    private synchronized void SetCustomData(double rawData, final int dataId) {
        switch (dataId) {
            case 0:
                Global.Custom0 = rawData;
                EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.Custom0, rawData));
                break;
            case 1:
                Global.Custom1 = rawData;
                EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.Custom1, rawData));
                break;
            case 2:
                Global.Custom2 = rawData;
                EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.Custom2, rawData));
                break;
            case 3:
                Global.Custom3 = rawData;
                EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.Custom3, rawData));
                break;
            case 4:
                Global.Custom4 = rawData;
                EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.Custom4, rawData));
                break;
            case 5:
                Global.Custom5 = rawData;
                EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.Custom5, rawData));
                break;
            case 6:
                Global.Custom6 = rawData;
                EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.Custom6, rawData));
                break;
            case 7:
                Global.Custom7 = rawData;
                EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.Custom7, rawData));
                break;
            case 8:
                Global.Custom8 = rawData;
                EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.Custom8, rawData));
                break;
            case 9:
                Global.Custom9 = rawData;
                EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.Custom9, rawData));
                break;
            default:
                break;
        }
    }

    private synchronized void CalculateWattHours(double volts, double amps, long dt_millis) {
        double wh = volts * amps * dt_millis // watt-milliseconds
                / 1000 / 60 / 60; // watt-hours

        Global.WattHours += wh;
        WattHourAvg.add(wh);
    }

    private synchronized void CalculateDistanceMeters(double speedMPS, long dt_millis) {
        double deltaMeters = speedMPS * dt_millis / 1000;

        Global.DistanceMeters += deltaMeters;

        double newWH = WattHourAvg.getAverage() / deltaMeters;

        Global.WattHoursPerMeter = newWH;

        if (Global.Lap > 0) {
            Global.LapDataList.get(Global.Lap - 1).AddDistanceMeters(deltaMeters);
            Global.LapDataList.get(Global.Lap - 1).AddWattHours(WattHourAvg.getAverage());
        }

        WattHourAvg.reset();

        EventBus.getDefault().post(new ArduinoEvent(ArduinoEvent.EventType.WattHours, newWH));
    }
}