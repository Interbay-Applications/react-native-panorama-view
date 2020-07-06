import * as React from "react";
import { ViewStyle } from "react-native";
interface Props {
    imageUrl: string;
    imageData: string;
    dimensions?: {
        width: number;
        height: number;
    };
    inputType?: "mono" | "stereo";
    enableTouchTracking?: boolean;
    onImageLoadingFailed?: () => void;
    onImageLoaded?: () => void;
    style: ViewStyle;
}
export declare const PanoramaView: React.FC<Props>;
export {};
