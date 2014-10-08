/*
 * Copyright (c) 2014, Tim Verbelen
 * Internet Based Communication Networks and Services research group (IBCN),
 * Department of Information Technology (INTEC), Ghent University - iMinds.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *    - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *    - Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *    - Neither the name of Ghent University - iMinds, nor the names of its 
 *      contributors may be used to endorse or promote products derived from 
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */
package be.iminds.androsgi.log;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

import android.util.Log;

public class AndroidLogService implements LogService {

	private static final String TAG = "OSGi";
	
	@Override
	public void log(int level, String message) {
		switch(level){
		case LogService.LOG_DEBUG:
			Log.d(TAG, message);
			break;
		case LogService.LOG_ERROR:
			Log.e(TAG, message);
			break;
		case LogService.LOG_INFO:
			Log.i(TAG, message);
			break;
		case LogService.LOG_WARNING:
			Log.w(TAG, message);
			break;
		}
	}

	@Override
	public void log(int level, String message, Throwable exception) {
		switch(level){
		case LogService.LOG_DEBUG:
			Log.d(TAG, message, exception);
			break;
		case LogService.LOG_ERROR:
			Log.e(TAG, message, exception);
			break;
		case LogService.LOG_INFO:
			Log.i(TAG, message, exception);
			break;
		case LogService.LOG_WARNING:
			Log.w(TAG, message, exception);	
			break;
		}
	}

	@Override
	public void log(ServiceReference sr, int level, String message) {
		log(level, message);
	}

	@Override
	public void log(ServiceReference sr, int level, String message,
			Throwable exception) {
		log(level, message, exception);
	}

}
