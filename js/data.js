(function(root){
'use strict';
function safeAudio(p){return typeof p==='string' && /^MP3\/.+\.mp3$/i.test(p) && p.slice(0,4)==='MP3/' && !/[\\?#%\x00-\x1f]/.test(p) && p.split('/').every(function(s){return s && s!=='.' && s!=='..';});}
function validate(q){
 if(!q||q.schemaVersion!==1)throw Error('Поддерживается только формат schemaVersion: 1.');
 if(typeof q.id!=='string'|| !/^[a-zA-Z0-9_-]+$/.test(q.id))throw Error('Некорректный идентификатор подборки.');
 if(typeof q.title!=='string'||!q.title.trim())throw Error('Укажите название подборки.');
 ['class','description'].forEach(function(k){if(q[k]!==undefined&&typeof q[k]!=='string')throw Error('Поле '+k+' должно быть текстом.');});
 if(!Array.isArray(q.tracks)||!q.tracks.length)throw Error('Добавьте хотя бы одно произведение.');
 var ids={};q.tracks.forEach(function(t,i){if(!t||typeof t.id!=='string'||!t.id||ids[t.id])throw Error('Повторяющийся или пустой ID произведения '+(i+1));ids[t.id]=true;
 ['composer','work'].forEach(function(k){if(typeof t[k]!=='string'||!t[k].trim())throw Error('Произведение '+(i+1)+': заполните композитора и название.');});
 if(t.part!==undefined&&typeof t.part!=='string')throw Error('Часть должна быть текстом.');
 if(!safeAudio(t.audio))throw Error('Произведение '+(i+1)+': выберите безопасный путь MP3/имя.mp3.');});return q;
}
function shuffle(a){a=a.slice();for(var i=a.length-1;i>0;i--){var j=Math.floor(Math.random()*(i+1)),t=a[i];a[i]=a[j];a[j]=t;}return a;}
function assign(s,track,card){if(s.finished||s.locked[track])return;s.attempts++;var ok=track===card;if(s.mode==='training'){if(ok){s.answers[track]=card;s.locked[track]=true;}else{s.errors++;delete s.answers[track];}}else{s.answers[track]=card;if(s.feedback==='immediate'&&!ok)s.errors++;}return ok;}
var api={validate:validate,safeAudio:safeAudio,shuffle:shuffle,assign:assign};if(typeof module!=='undefined')module.exports=api;root.QuizData=api;
})(typeof window!=='undefined'?window:globalThis);
