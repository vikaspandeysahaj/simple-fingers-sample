package com.neurotec.samples;

import netscape.javascript.JSException;
import netscape.javascript.JSObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.applet.Applet;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vikas on 19/01/15.
 */
public class FingerPrintService {

   private Applet applet;
    public FingerPrintService(Applet appletWindow)
    {
        applet = appletWindow;
    }
    public java.util.List<PatientFingerPrintModel> getAllPatients() throws JSONException {

        String JSONPatients = callGetAllPatientJavascriptFunction();
        List<PatientFingerPrintModel> patients = PatientFingerPrintModelBuilder(JSONPatients);
        return patients;

    }

    public void sendIdentifiedPatientID(String patientID) {

        callSendIdentifiedPatientIDJavaScriptFunction(patientID);
    }

    //to converter class

    private List<PatientFingerPrintModel> PatientFingerPrintModelBuilder(String jsonPatients) throws JSONException {
        List<PatientFingerPrintModel> patients = new ArrayList<PatientFingerPrintModel>();
        JSONArray jsonArray = new JSONArray(jsonPatients);
        for (int i = 0; i< jsonArray.length(); i++){
            JSONObject object = jsonArray.getJSONObject(i);
            PatientFingerPrintModel patient = new PatientFingerPrintModel(object.getString("patientUUID"), object.getString("fingerprintTemplate"));
            patients.add(patient);
        }
        return patients;
    }




    //to JAva script class
    public String callGetAllPatientJavascriptFunction(){
        try {
            JSObject window = JSObject.getWindow(applet);
            String strdata = (String)window.call("GetAllPatient", null);
            return strdata;

        } catch (JSException jse) {
            jse.printStackTrace();
        }
        return null;
    }
    public void callSendIdentifiedPatientIDJavaScriptFunction(String patientID)
    {
        try {
            JSObject window = JSObject.getWindow(applet);
            window.call("SendIdentifiedPatientID", new Object[] {patientID});
        } catch (JSException jse) {
            jse.printStackTrace();
        }
    }

    public String alertMessage() {
        try {

            JSObject window = JSObject.getWindow(applet);
            String str = (String)window.call("showMessage", null);
            return str;
        } catch (JSException jse) {
            jse.printStackTrace();
        }
        return null;
    }
}
