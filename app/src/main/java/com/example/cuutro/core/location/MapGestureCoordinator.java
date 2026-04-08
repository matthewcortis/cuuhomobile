package com.example.cuutro.core.location;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class MapGestureCoordinator {

    public interface InteractionListener {
        void onMapInteractionChanged(boolean isInteracting);
    }

    private MapGestureCoordinator() {
    }

    public static void install(@NonNull View mapView) {
        install(mapView, null);
    }

    public static void install(
            @NonNull View mapView,
            @Nullable InteractionListener interactionListener
    ) {
        mapView.setOnTouchListener(new View.OnTouchListener() {
            private boolean isLocked;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event == null) {
                    return false;
                }

                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN
                        || action == MotionEvent.ACTION_POINTER_DOWN
                        || action == MotionEvent.ACTION_MOVE) {
                    if (!isLocked) {
                        isLocked = true;
                        updateParentInterception(view, true, interactionListener);
                    }
                } else if (action == MotionEvent.ACTION_UP
                        || action == MotionEvent.ACTION_CANCEL
                        || action == MotionEvent.ACTION_OUTSIDE) {
                    if (isLocked) {
                        isLocked = false;
                        updateParentInterception(view, false, interactionListener);
                    }
                }
                return false;
            }
        });
    }

    private static void updateParentInterception(
            @NonNull View view,
            boolean disallowParentIntercept,
            @Nullable InteractionListener interactionListener
    ) {
        ViewParent parent = view.getParent();
        while (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallowParentIntercept);
            parent = parent.getParent();
        }

        if (interactionListener != null) {
            interactionListener.onMapInteractionChanged(disallowParentIntercept);
        }
    }
}
