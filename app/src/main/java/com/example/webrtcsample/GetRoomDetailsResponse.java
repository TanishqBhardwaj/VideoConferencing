package com.example.webrtcsample;

import java.util.List;

public class GetRoomDetailsResponse {

    private OfferSdp offerSdp;
    private List<Candidate> callerCandidates;

    public static class  OfferSdp {
        private String type;
        private String sdp;

        public OfferSdp(String type, String sdp) {
            this.type = type;
            this.sdp = sdp;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getSdp() {
            return sdp;
        }

        public void setSdp(String sdp) {
            this.sdp = sdp;
        }
    }

    public static class Candidate {
        private String sdpMid;
        private int sdpMLineIndex;
        private String candidate;

        public Candidate(String sdpMid, int sdpMLineIndex, String candidate) {
            this.sdpMid = sdpMid;
            this.sdpMLineIndex = sdpMLineIndex;
            this.candidate = candidate;
        }

        public String getSdpMid() {
            return sdpMid;
        }

        public void setSdpMid(String sdpMid) {
            this.sdpMid = sdpMid;
        }

        public int getSdpMLineIndex() {
            return sdpMLineIndex;
        }

        public void setSdpMLineIndex(int sdpMLineIndex) {
            this.sdpMLineIndex = sdpMLineIndex;
        }

        public String getCandidate() {
            return candidate;
        }

        public void setCandidate(String candidate) {
            this.candidate = candidate;
        }
    }

    public GetRoomDetailsResponse(OfferSdp offerSdp, List<Candidate> callerCandidates) {
        this.offerSdp = offerSdp;
        this.callerCandidates = callerCandidates;
    }

    public OfferSdp getOfferSdp() {
        return offerSdp;
    }

    public void setOfferSdp(OfferSdp offerSdp) {
        this.offerSdp = offerSdp;
    }

    public List<Candidate> getCallerCandidates() {
        return callerCandidates;
    }

    public void setCallerCandidates(List<Candidate> callerCandidates) {
        this.callerCandidates = callerCandidates;
    }
}
