#Used for testing light configs (mostly now for the light effects in WledController.kt)

WLED_IP="192.168.1.67" # The office lights


PREV_STATE=$(curl -s "http://${WLED_IP}/json/state")

curl -s -X POST "http://${WLED_IP}/json/state" \
  -H "Content-Type: application/json" \
  -d '{"on": true,
          "bri": 220,
          "tt": 10,
          "seg": [{
            "fx": 115,
            "pal": 10,
            "sx": 35,
            "ix": 50,
            "grp": 1,
            "spc": 0,
            "mi": false,
            "rev": false
          }]}' > /dev/null

sleep 3

# Replay the original state
curl -s -X POST "http://${WLED_IP}/json/state" \
  -H "Content-Type: application/json" \
  -d "$PREV_STATE" > /dev/null