package org.starexec.data.to;

import com.google.gson.annotations.Expose;

public class SolverBuildStatus {
        public static enum SolverBuildStatusCode {
                UNBUILT(0),
                BUILT(1),
                BUILT_BY_STAREXEC(2),
                BUILD_FAILED(3),
                STATUS_UNKNOWN(4);

                private int val;
                
                private SolverBuildStatusCode(int val) {
                        this.val = val;
                }

                public int getVal() {
                        return this.val;
                }

                public static SolverBuildStatusCode toStatusCode(int code) {

                        switch (code) {
                                case 0:
                                        return SolverBuildStatusCode.UNBUILT;
                                case 1:
                                        return SolverBuildStatusCode.BUILT;
                                case 2:
                                        return SolverBuildStatusCode.BUILT_BY_STAREXEC;
                                case 3:
                                        return SolverBuildStatusCode.BUILD_FAILED;
                        }
                        return SolverBuildStatusCode.STATUS_UNKNOWN;
                }
        }

        private static String getDescription(int code) {
                switch (code) {
                        case 0:
                                return "This solver is not built yet.";
                        case 1:
                                return "This solver was compiled prior to uploading to StarExec.";
                        case 2:
                                return "This solver has been built by StarExec.";

                        case 3:
                                return "StarExec has attempted to build this solver and failed.";
                }
                return "This solver has an unknown build status code";
        }

        private static String getStatus(int code) {
                switch (code) {
                        case 0:
                                return "unbuilt";
                        case 1:
                                return "uploaded built";
                        case 2:
                                return "built on StarExec";
                        case 3:
                                return "build failed";
                }
                return "unknown";
        }


        @Expose private SolverBuildStatusCode code = SolverBuildStatusCode.STATUS_UNKNOWN;

        public SolverBuildStatusCode getCode() {
                return code;
        }

        public void setCode(SolverBuildStatusCode code) {
                this.code = code;
        }

        public void setCode(int code) {
                this.code = SolverBuildStatusCode.toStatusCode(code);
        }

        public String getStatus() {
                return SolverBuildStatus.getStatus(this.code.getVal());
        }

        public String getDescription() {
                return SolverBuildStatus.getDescription(this.code.getVal());
        }

        @Override
        public String toString() {
                return this.getStatus();
        }

}

