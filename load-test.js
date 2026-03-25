import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 100,
    duration: '30s',
};

export default function () {
    const res = http.get('http://localhost:8080/api/request', {
        headers: { 'X-User-Id': 'sevara' },
    });

    check(res, {
        'status is 200 or 429': (r) => r.status === 200 || r.status === 429,
    });
}