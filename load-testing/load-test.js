import http from 'k6/http';
import { sleep, check } from 'k6';

export const options = {
  stages: [
    {duration: '2m', target: 300},
    {duration: '4m', target: 350},
    {duration: '2m', target: 0},
  ]
};

export default function() {
  const eventId = "e58ed763-928c-4155-bee9-fdbaaadc15f3";
  const baseUrl = "http://localhost/api";
  // Alternative without ingress/tunnel: kubectl port-forward svc/ticket-api 8080:8080 -n ticket-system
  // const baseUrl = "http://127.0.0.1:8080/api";
  const quantity = Math.floor(Math.random() * 5) + 1;
  const availabilityPause = Math.random() * 2 + 0.5;
  const orderPause = Math.random() * 3 + 0.5;

  // 1. Check ticket availability
  let availRes = http.get(`${baseUrl}/events/${eventId}/tickets/availability`);
  check(availRes, { 
    "availability status is 200": (r) => r.status === 200 
  });
  
  sleep(availabilityPause);

  // 2. Place a ticket order
  const orderPayload = JSON.stringify({
    userId: "7d9f2e1a-4b3c-4d5e-8f6a-9b0c1d2e3f4a",
    quantity,
    requestId: `req-${__VU}-${__ITER}`
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  let orderRes = http.post(`${baseUrl}/events/${eventId}/tickets/order`, orderPayload, params);
  
  check(orderRes, { 
    "order status is 202": (r) => r.status === 202 
  });

  sleep(orderPause);
}
