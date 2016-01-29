package by.aleks.chordbro.api;

import android.app.Activity;
import by.aleks.chordbro.R;
import by.aleks.chordbro.views.VisualizerView;
import com.gracenote.gnsdk.IGnAudioSource;

import java.nio.ByteBuffer;

/**
 * Created by Alex on 1/29/16.
 */

/**
 * Audio visualization adapter.
 * Sits between GnMic and GnMusicIdStream to receive audio data as it
 * is pulled from the microphone allowing an audio visualization to be
 * implemented.
 */
class AudioVisualizeAdapter implements IGnAudioSource {

    private IGnAudioSource 	audioSource;
    private int				numBitsPerSample;
    private int				numChannels;
    private VisualizerView view;
    private Activity activity;

    public AudioVisualizeAdapter(IGnAudioSource audioSource, Activity activity){
        this.audioSource = audioSource;
        this.activity = activity;
        view = (VisualizerView) activity.findViewById(R.id.visualizer);
    }

    @Override
    public long sourceInit() {
        if ( audioSource == null ){
            return 1;
        }
        long retVal = audioSource.sourceInit();

        // get format information for use later
        if ( retVal == 0 ) {
            numBitsPerSample = (int)audioSource.sampleSizeInBits();
            numChannels = (int)audioSource.numberOfChannels();
        }

        return retVal;
    }

    @Override
    public long numberOfChannels() {
        return numChannels;
    }

    @Override
    public long sampleSizeInBits() {
        return numBitsPerSample;
    }

    @Override
    public long samplesPerSecond() {
        if ( audioSource == null ){
            return 0;
        }
        return audioSource.samplesPerSecond();
    }

    @Override
    public long getData(ByteBuffer buffer, long bufferSize) {
        if ( audioSource == null ){
            return 0;
        }

        long numBytes = audioSource.getData(buffer, bufferSize);

        if ( numBytes != 0 ) {
            final byte[] bytes = new byte[(int)numBytes];
            for (int i=0; i<numBytes; i++){
                bytes[i] = buffer.get();
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    view.updateVisualizer(bytes);
                }
            });
        }

        return numBytes;
    }

    @Override
    public void sourceClose() {
        if ( audioSource != null ){
            audioSource.sourceClose();
        }
    }
}