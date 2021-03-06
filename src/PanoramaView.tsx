import * as React from "react";
import { requireNativeComponent, ViewStyle, Platform } from "react-native";

interface Props {
  imageUrl: string;
  imageData: string;
  dimensions?: { width: number; height: number }; // Android-only
  inputType?: "mono" | "stereo"; // Android-only
  enableTouchTracking?: boolean;
  onImageLoadingFailed?: () => void;
  onImageLoaded?: () => void;
  style: ViewStyle;
}

export const PanoramaView: React.FC<Props> = ({
  onImageLoadingFailed,
  onImageLoaded,
  dimensions,
  inputType,
  ...props
}) => {
  const _onImageLoadingFailed = () => {
    if (onImageLoadingFailed) {
      onImageLoadingFailed();
    }
  };

  const _onImageLoaded = () => {
    if (onImageLoaded) {
      onImageLoaded();
    }
  };

  if (Platform.OS === "android" && !dimensions) {
    console.warn('The "dimensions" property is required for PanoramaView on Android devices.');
    return null;
  }

  if (Platform.OS === "ios" && inputType === "stereo") {
    console.warn("The stereo inputType is currently only supported on Android devices.");
  }

  return (
    <NativePanoramaView
      {...props}
      dimensions={dimensions}
      inputType={inputType}
      onImageLoaded={_onImageLoaded}
      onImageLoadingFailed={_onImageLoadingFailed}
    />
  );
};

const NativePanoramaView = requireNativeComponent("PanoramaView");
